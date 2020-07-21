package software.amazon.ec2.prefixlist;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.ec2.prefixlist.TestHelper.CONTEXT_MUTATION_STARTED;
import static software.amazon.ec2.prefixlist.TestHelper.DELETE_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.DELETE_RESULT;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_DELETE_FAILED;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_DELETE_IN_PROGRESS;
import static software.amazon.ec2.prefixlist.TestHelper.INVALID_PREFIX_LIST_ID_NOT_FOUND;
import static software.amazon.ec2.prefixlist.TestHelper.INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_MODEL_CREATED;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
    }
    @AfterEach
    public void after() {
        verifyNoMoreInteractions(proxy, logger);
    }

    @Test
    public void handleRequestWithMutationNotStarted() {
        when(proxy.injectCredentialsAndInvoke(eq(DELETE_REQUEST), any())).thenReturn(DELETE_RESULT);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, null, logger);

        verify(logger).log(any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequestWithMutationNotStartedAndPrefixListNotFound() {
        INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION.setErrorCode(INVALID_PREFIX_LIST_ID_NOT_FOUND);
        when(proxy.injectCredentialsAndInvoke(eq(DELETE_REQUEST), any())).thenThrow(INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION);

        Assertions.assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, null, logger);
        });
    }

    @Test
    public void handleRequestWithMutationStartedAndPrefixListIdNotFound() {
        INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION.setErrorCode(INVALID_PREFIX_LIST_ID_NOT_FOUND);
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenThrow(INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION);
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_STARTED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequestWithMutationStartedAndDeleteInProgressState() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_DELETE_IN_PROGRESS);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_STARTED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(TestHelper.POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequestWithMutationStartedAndDeleteFailedState() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_DELETE_FAILED);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_STARTED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED);
        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
