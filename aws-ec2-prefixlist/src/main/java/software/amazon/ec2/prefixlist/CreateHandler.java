package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AddPrefixListEntry;
import com.amazonaws.services.ec2.model.CreateManagedPrefixListRequest;
import com.amazonaws.services.ec2.model.CreateManagedPrefixListResult;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsRequest;
import com.amazonaws.services.ec2.model.ManagedPrefixList;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.google.common.collect.ImmutableList;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        final AmazonEC2 ec2Client = ClientBuilder.getClient();
        final ResourceModel model = request.getDesiredResourceState();
        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext.toBuilder().build();

        if (!context.isMutationStarted()) {
            List<AddPrefixListEntry> addPrefixListEntries = ModuleHelper.convertToAddPrefixListEntries(model.getEntries());
            List<TagSpecification> tagSpecifications = ModuleHelper.convertToTagSpecifications(model.getTags());
            final CreateManagedPrefixListRequest createRequest = new CreateManagedPrefixListRequest()
                    .withPrefixListName(model.getPrefixListName())
                    .withMaxEntries(model.getMaxEntries())
                    .withAddressFamily(model.getAddressFamily())
                    .withEntries(addPrefixListEntries)
                    .withTagSpecifications(tagSpecifications);

            final CreateManagedPrefixListResult createResult = proxy.injectCredentialsAndInvoke(createRequest, ec2Client::createManagedPrefixList);
            context.setPrefixListId(createResult.getPrefixList().getPrefixListId());
            context.setMutationStarted(true);
            logger.log(String.format("Prefix list with PrefixListId %s is successfully created.", createResult.getPrefixList().getPrefixListId()));

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(context)
                    .resourceModel(model)
                    .status(OperationStatus.IN_PROGRESS)
                    .build();
        }

        final DescribeManagedPrefixListsRequest describeRequest = new DescribeManagedPrefixListsRequest()
                .withPrefixListIds(ImmutableList.of(context.getPrefixListId()));
        final ManagedPrefixList prefixList = proxy.injectCredentialsAndInvoke(describeRequest, ec2Client::describeManagedPrefixLists)
                .getPrefixLists()
                .get(0);
        if(prefixList.getState().equals(ModuleHelper.CREATE_FAILED)) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(context)
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .build();
        } else if(prefixList.getState().equals(ModuleHelper.CREATE_IN_PROGRESS)) {
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
                    .prefixListId(prefixList.getPrefixListId())
                    .ownerId(prefixList.getOwnerId())
                    .version(prefixList.getVersion().intValue())
                    .arn(prefixList.getPrefixListArn())
                    .build();
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(updatedModel)
                    .status(OperationStatus.SUCCESS)
                    .build();
        }
    }
}
