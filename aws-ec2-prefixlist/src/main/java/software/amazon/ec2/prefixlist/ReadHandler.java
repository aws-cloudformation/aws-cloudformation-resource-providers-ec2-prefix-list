package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeAccountAttributesResult;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsRequest;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsResult;
import com.amazonaws.services.ec2.model.GetManagedPrefixListEntriesRequest;
import com.amazonaws.services.ec2.model.GetManagedPrefixListEntriesResult;
import com.amazonaws.services.ec2.model.ManagedPrefixList;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final AmazonEC2 ec2Client = ClientBuilder.getClient();
        final ResourceModel model = request.getDesiredResourceState();
        final String prefixListId = model.getPrefixListId();

        final DescribeManagedPrefixListsRequest describeRequest = new DescribeManagedPrefixListsRequest().withPrefixListIds(prefixListId);
        final GetManagedPrefixListEntriesRequest getEntriesRequest = new GetManagedPrefixListEntriesRequest().withPrefixListId(prefixListId);

        final DescribeManagedPrefixListsResult describeResult = ModuleHelper.invokeAndConvertException(() ->
                proxy.injectCredentialsAndInvoke(describeRequest, ec2Client::describeManagedPrefixLists),
                prefixListId);

        final GetManagedPrefixListEntriesResult getEntriesResult = ModuleHelper.invokeAndConvertException(() ->
                proxy.injectCredentialsAndInvoke(getEntriesRequest, ec2Client::getManagedPrefixListEntries),
                prefixListId);

        final List<Entry> entries = ModuleHelper.convertToEntries(getEntriesResult.getEntries());
        final ManagedPrefixList prefixList = describeResult.getPrefixLists().get(0);

        final ResourceModel currentModel = ResourceModel.builder()
                .prefixListId(prefixListId)
                .prefixListName(prefixList.getPrefixListName())
                .addressFamily(prefixList.getAddressFamily())
                .maxEntries(prefixList.getMaxEntries())
                .version(prefixList.getVersion().intValue())
                .entries(entries)
                .tags(ModuleHelper.convertToResourceModelTags(prefixList.getTags()))
                .ownerId(prefixList.getOwnerId())
                .arn(prefixList.getPrefixListArn())
                .build();
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(currentModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
