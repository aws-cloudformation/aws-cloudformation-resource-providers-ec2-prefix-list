package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.model.CreateManagedPrefixListRequest;
import com.amazonaws.services.ec2.model.CreateManagedPrefixListResult;
import org.junit.jupiter.api.AfterEach;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.ec2.prefixlist.TestHelper.ADD_ENTRIES;
import static software.amazon.ec2.prefixlist.TestHelper.CONTEXT_MUTATION_STARTED_WITH_PREFIX_LIST_ID;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_CREATE_COMPLETE;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_CREATE_FAILED;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_CREATE_IN_PROGRESS;
import static software.amazon.ec2.prefixlist.TestHelper.PREFIX_LIST;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_HANDLER_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_MODEL;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_MODEL_CREATED;
import static software.amazon.ec2.prefixlist.TestHelper.convertToTagSpecifications;

@ExtendWith(MockitoExtension.class)
public class
CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
    }

    @AfterEach
    public void after() {
        verifyNoMoreInteractions(proxy, logger);
    }

    @Test
    public void handleRequestWithMutationNotStarted() {
        final CreateManagedPrefixListRequest createRequest = new CreateManagedPrefixListRequest()
                .withMaxEntries(RESOURCE_MODEL.getMaxEntries())
                .withPrefixListName(RESOURCE_MODEL.getPrefixListName())
                .withAddressFamily(RESOURCE_MODEL.getAddressFamily())
                .withEntries(ADD_ENTRIES)
                .withTagSpecifications(convertToTagSpecifications(RESOURCE_MODEL.getTags()));

        final CreateManagedPrefixListResult result = new CreateManagedPrefixListResult().withPrefixList(PREFIX_LIST);
        when(proxy.injectCredentialsAndInvoke(eq(createRequest), any())).thenReturn(result);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED_WITH_PREFIX_LIST_ID);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_HANDLER_REQUEST.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        verify(logger).log(any());
    }

    @Test
    public void handlerRequestWithMutationStartedAndCreateCompleteState() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_CREATE_COMPLETE);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, CONTEXT_MUTATION_STARTED_WITH_PREFIX_LIST_ID, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handlerRequestWithMutationStartedAndCreateInProgressState() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_CREATE_IN_PROGRESS);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, CONTEXT_MUTATION_STARTED_WITH_PREFIX_LIST_ID, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED_WITH_PREFIX_LIST_ID);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(TestHelper.POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_HANDLER_REQUEST.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handlerRequestWithMutationStartedAndCreateFailedState() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_CREATE_FAILED);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST, CONTEXT_MUTATION_STARTED_WITH_PREFIX_LIST_ID, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED_WITH_PREFIX_LIST_ID);
        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_HANDLER_REQUEST.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

}
