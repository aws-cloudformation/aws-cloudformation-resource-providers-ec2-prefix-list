package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsRequest;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsResult;
import com.amazonaws.services.ec2.model.GetManagedPrefixListEntriesRequest;
import com.amazonaws.services.ec2.model.GetManagedPrefixListEntriesResult;
import com.amazonaws.services.ec2.model.PrefixListEntry;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final AmazonEC2 ec2Client = ClientBuilder.getClient();

        final List<ResourceModel> models = new ArrayList<>();

        String nextToken = null;

        do {
            final DescribeManagedPrefixListsRequest describeRequest = new DescribeManagedPrefixListsRequest()
                    .withNextToken(nextToken);
            final DescribeManagedPrefixListsResult describeResult  = proxy.injectCredentialsAndInvoke(describeRequest, ec2Client::describeManagedPrefixLists);

            models.addAll(describeResult.getPrefixLists()
                    .stream()
                    .map(prefixList -> ResourceModel.builder()
                            .prefixListId(prefixList.getPrefixListId())
                            .prefixListName(prefixList.getPrefixListName())
                            .maxEntries(prefixList.getMaxEntries())
                            .addressFamily(prefixList.getAddressFamily())
                            .version(prefixList.getVersion() == null ? 1 : prefixList.getVersion().intValue())
                            .entries(getPrefixListEntries(proxy, ec2Client, prefixList.getPrefixListId()))
                            .tags(ModuleHelper.convertToResourceModelTags(prefixList.getTags()))
                            .arn(prefixList.getPrefixListArn())
                            .ownerId(prefixList.getOwnerId())
                            .build())
                    .collect(Collectors.toList()));
            nextToken = describeResult.getNextToken();
        } while (nextToken != null);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }
    private List<Entry> getPrefixListEntries(AmazonWebServicesClientProxy proxy, AmazonEC2 ec2Client, String prefixListId) {
        String nextToken = null;
        List<PrefixListEntry> entryList = new ArrayList<>();

        do {
            final GetManagedPrefixListEntriesRequest getEntriesRequest = new GetManagedPrefixListEntriesRequest()
                    .withPrefixListId(prefixListId)
                    .withNextToken(nextToken);
            final GetManagedPrefixListEntriesResult getEntriesResult = proxy.injectCredentialsAndInvoke(getEntriesRequest, ec2Client::getManagedPrefixListEntries);
            nextToken = getEntriesResult.getNextToken();
            entryList.addAll(getEntriesResult.getEntries());
        } while (nextToken != null);

        return ModuleHelper.convertToEntries(entryList);
    }
}
