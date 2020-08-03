package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DeleteManagedPrefixListRequest;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsRequest;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsResult;
import com.amazonaws.services.ec2.model.ManagedPrefixList;
import com.google.common.collect.ImmutableList;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        final AmazonEC2 ec2Client = ClientBuilder.getClient();
        final ResourceModel model = request.getDesiredResourceState();
        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext.toBuilder().build();
        final String prefixListId = model.getPrefixListId();

        if (!context.isMutationStarted()) {
            final DeleteManagedPrefixListRequest deleteRequest = new DeleteManagedPrefixListRequest().withPrefixListId(prefixListId);

            ModuleHelper.invokeAndConvertException(() ->
                    proxy.injectCredentialsAndInvoke(deleteRequest, ec2Client::deleteManagedPrefixList),
                    prefixListId);

            context.setMutationStarted(true);
            logger.log(String.format("Deleting prefix list with PrefixListId %s.", prefixListId));

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(context)
                    .resourceModel(model)
                    .status(OperationStatus.IN_PROGRESS)
                    .build();
        }

        final DescribeManagedPrefixListsRequest describeRequest = new DescribeManagedPrefixListsRequest()
                .withPrefixListIds(ImmutableList.of(prefixListId));
        try {
            final DescribeManagedPrefixListsResult result = proxy.injectCredentialsAndInvoke(describeRequest, ec2Client::describeManagedPrefixLists);
            final ManagedPrefixList prefixList = result.getPrefixLists().get(0);
            if (prefixList.getState().equals(ModuleHelper.DELETE_FAILED)) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .callbackContext(context)
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .build();
            } else {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .callbackContext(context)
                        .callbackDelaySeconds(ModuleHelper.POLLING_DELAY_SECONDS)
                        .resourceModel(model)
                        .status(OperationStatus.IN_PROGRESS)
                        .build();
            }
        } catch (AmazonEC2Exception ex) {
            if (ModuleHelper.INVALID_PREFIX_LIST_ID_NOT_FOUND.equals(ex.getErrorCode())) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.SUCCESS)
                        .build();
            }
            throw ex;
        }
    }
}
