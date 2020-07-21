package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AddPrefixListEntry;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsRequest;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsResult;
import com.amazonaws.services.ec2.model.GetManagedPrefixListEntriesRequest;
import com.amazonaws.services.ec2.model.GetManagedPrefixListEntriesResult;
import com.amazonaws.services.ec2.model.ManagedPrefixList;
import com.amazonaws.services.ec2.model.ModifyManagedPrefixListRequest;
import com.amazonaws.services.ec2.model.PrefixListEntry;
import com.amazonaws.services.ec2.model.RemovePrefixListEntry;
import com.google.common.collect.ImmutableList;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        AmazonEC2 ec2Client = ClientBuilder.getClient();

        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext.toBuilder().build();

        final ResourceModel model = request.getDesiredResourceState();

        final String prefixListId = model.getPrefixListId();

        final DescribeManagedPrefixListsRequest describeRequest = new DescribeManagedPrefixListsRequest()
                .withPrefixListIds(ImmutableList.of(prefixListId));
        final DescribeManagedPrefixListsResult describeResult = ModuleHelper.invokeAndConvertException(() ->
                proxy.injectCredentialsAndInvoke(describeRequest, ec2Client::describeManagedPrefixLists),
                prefixListId);

        final ManagedPrefixList currentPrefixList  = describeResult.getPrefixLists().get(0);

        if (!currentPrefixList.getMaxEntries().equals(model.getMaxEntries()) || !currentPrefixList.getAddressFamily().equals(model.getAddressFamily())) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .message("MaxEntries or AddressFamily is not updatable.")
                    .build();
        }

        if (!context.isTagsUpdated()) {
            final List<com.amazonaws.services.ec2.model.Tag> finalTags = ModuleHelper.convertToEc2Tags(model.getTags());
            final List<com.amazonaws.services.ec2.model.Tag> currentTags = currentPrefixList.getTags() == null ? ImmutableList.of() :
                    currentPrefixList.getTags();
            updateTags(finalTags, currentTags, proxy, ec2Client, prefixListId);
            context.setTagsUpdated(true);
         }

        if (!context.isMutationStarted()) {
            final List<Entry> finalEntries = model.getEntries();
            final List<Entry> currentEntries = getCurrentEntries(proxy, ec2Client, prefixListId);
            final List<Entry> addEntries = getAddEntries(currentEntries, finalEntries);
            final List<Entry> removeEntries = getRemoveEntries(currentEntries, finalEntries);

            if (addEntries.isEmpty() && removeEntries.isEmpty()) {
                if (!currentPrefixList.getPrefixListName().equals(model.getPrefixListName())) {
                    final ModifyManagedPrefixListRequest modifyRequest = new ModifyManagedPrefixListRequest()
                            .withPrefixListId(model.getPrefixListId())
                            .withPrefixListName(model.getPrefixListName());
                    proxy.injectCredentialsAndInvoke(modifyRequest, ec2Client::modifyManagedPrefixList);
                }
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.SUCCESS)
                        .build();
            }

            final List<AddPrefixListEntry> addPrefixListEntries = ModuleHelper.convertToAddPrefixListEntries(addEntries);
            final List<RemovePrefixListEntry> removePrefixListEntries = ModuleHelper.convertToRemovePrefixListEntries(removeEntries);

            final ModifyManagedPrefixListRequest modifyRequest = new ModifyManagedPrefixListRequest()
                    .withPrefixListId(prefixListId)
                    .withAddEntries(addPrefixListEntries)
                    .withRemoveEntries(removePrefixListEntries)
                    .withCurrentVersion(currentPrefixList.getVersion())
                    .withPrefixListName(model.getPrefixListName());
            proxy.injectCredentialsAndInvoke(modifyRequest, ec2Client::modifyManagedPrefixList);
            context.setMutationStarted(true);
            logger.log("Initiated Prefix List update request.");
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(context)
                    .resourceModel(model)
                    .status(OperationStatus.IN_PROGRESS)
                    .build();
        }

        // Return SUCCESS if mutation complete, IN_PROGRESS otherwise
        return confirmMutationComplete(model, context, currentPrefixList);
    }

    private List<Entry> getAddEntries(final List<Entry> currentEntries, final List<Entry> finalEntries) {
        final Set<Entry> entrySet = new HashSet<Entry>();
        entrySet.addAll(currentEntries);
        return finalEntries.stream()
                .filter(entry -> !entrySet.contains(entry))
                .collect(Collectors.toList());
    }

    private List<Entry> getRemoveEntries(final List<Entry> currentEntries, final List<Entry> finalEntries) {
        final Set<String> entrySet = new HashSet<String>();
        finalEntries.stream()
                .forEach(entry -> entrySet.add(entry.getCidr()));
        return currentEntries.stream()
                .filter(entry -> !entrySet.contains(entry.getCidr()))
                .collect(Collectors.toList());
    }

    private List<Entry> getCurrentEntries(final AmazonWebServicesClientProxy proxy, final AmazonEC2 ec2Client, final String prefixListId) {
        String nextToken = null;
        List<PrefixListEntry> currentPrefixListEntries = new ArrayList<PrefixListEntry>();
        do {
            final GetManagedPrefixListEntriesRequest getEntriesRequest = new GetManagedPrefixListEntriesRequest()
                    .withPrefixListId(prefixListId)
                    .withNextToken(nextToken);
            final GetManagedPrefixListEntriesResult result = ModuleHelper.invokeAndConvertException(() ->
                    proxy.injectCredentialsAndInvoke(getEntriesRequest, ec2Client::getManagedPrefixListEntries),
                    prefixListId);
            nextToken = result.getNextToken();
            currentPrefixListEntries.addAll(result.getEntries());
        } while (nextToken != null);
        return ModuleHelper.convertToEntries(currentPrefixListEntries);
    }

    /*
     * This Method will return SUCCESS status if mutation is complete, IN_PROGRESS otherwise
     */
    private ProgressEvent<ResourceModel, CallbackContext> confirmMutationComplete(
            final ResourceModel model,
            final CallbackContext context,
            final ManagedPrefixList currentPrefixList) {
        if (currentPrefixList.getState().equals(ModuleHelper.MODIFY_FAILED)) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(context)
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .build();
        }else if (currentPrefixList.getState().equals(ModuleHelper.MODIFY_IN_PROGRESS)) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(context)
                    .callbackDelaySeconds(ModuleHelper.POLLING_DELAY_SECONDS)
                    .resourceModel(model)
                    .status(OperationStatus.IN_PROGRESS)
                    .build();
        } else {
            final ResourceModel updatedModel = ResourceModel.builder()
                    .entries(model.getEntries())
                    .tags(model.getTags())
                    .addressFamily(model.getAddressFamily())
                    .maxEntries(model.getMaxEntries())
                    .prefixListName(model.getPrefixListName())
                    .prefixListId(model.getPrefixListId())
                    .ownerId(model.getOwnerId())
                    .arn(model.getArn())
                    .version(currentPrefixList.getVersion().intValue())
                    .build();
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(updatedModel)
                    .status(OperationStatus.SUCCESS)
                    .build();
        }
    }

    private void updateTags(
            final List<com.amazonaws.services.ec2.model.Tag> finalTags,
            final List<com.amazonaws.services.ec2.model.Tag> currentTags,
            final AmazonWebServicesClientProxy proxy,
            final AmazonEC2 ec2Client,
            final String prefixListId) {
        if (finalTags.size() != currentTags.size() || !finalTags.containsAll(currentTags)) {
            if(!currentTags.isEmpty()) {
                final DeleteTagsRequest deleteTagsRequest = new DeleteTagsRequest()
                        .withResources(prefixListId)
                        .withTags(currentTags);
                proxy.injectCredentialsAndInvoke(deleteTagsRequest, ec2Client::deleteTags);
            }
            if (!finalTags.isEmpty()) {
                final CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                        .withResources(prefixListId)
                        .withTags(finalTags);
                proxy.injectCredentialsAndInvoke(createTagsRequest, ec2Client::createTags);
            }
        }
        return;
    }
}
