package software.amazon.ec2.prefixlist;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_WITH_TAGS;
import static software.amazon.ec2.prefixlist.TestHelper.GET_ENTRIES_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.GET_ENTRIES_RESULT_NO_MODIFICATION;
import static software.amazon.ec2.prefixlist.TestHelper.INVALID_PREFIX_LIST_ID_NOT_FOUND;
import static software.amazon.ec2.prefixlist.TestHelper.INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_MODEL_CREATED;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
    }
    @AfterEach
    public void after() {
        verifyNoMoreInteractions(proxy, logger);
    }

    @Test
    public void handleRequesSimpleSuccess() {
        doReturn(DESCRIBE_RESULT_WITH_TAGS).when(proxy).injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST),any());
        doReturn(GET_ENTRIES_RESULT_NO_MODIFICATION).when(proxy).injectCredentialsAndInvoke(eq(GET_ENTRIES_REQUEST),any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test()
    public void handleRequestWithPrefixListNotFound() {
        INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION.setErrorCode(INVALID_PREFIX_LIST_ID_NOT_FOUND);
        doThrow(INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION).when(proxy).injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any());
        Assertions.assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, null, logger);
        });
    }
}
