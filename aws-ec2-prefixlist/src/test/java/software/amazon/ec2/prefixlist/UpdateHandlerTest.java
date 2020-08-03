package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.model.ModifyManagedPrefixListRequest;
import com.amazonaws.services.ec2.model.ModifyManagedPrefixListResult;
import com.google.common.collect.ImmutableList;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.ec2.prefixlist.TestHelper.ADD_PREFIX_LIST_ENTRY_1_MODIFIED;
import static software.amazon.ec2.prefixlist.TestHelper.ADD_PREFIX_LIST_ENTRY_2;
import static software.amazon.ec2.prefixlist.TestHelper.CONTEXT_MUTATION_NOT_STARTED;
import static software.amazon.ec2.prefixlist.TestHelper.CONTEXT_MUTATION_STARTED_AND_TAGS_UPDATED;
import static software.amazon.ec2.prefixlist.TestHelper.CONTEXT_TAGS_UPDATED;
import static software.amazon.ec2.prefixlist.TestHelper.CREATE_TAGS_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.CREATE_TAGS_RESULT;
import static software.amazon.ec2.prefixlist.TestHelper.DELETE_TAGS_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.DELETE_TAGS_RESULT;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_MODIFY_COMPLETE;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_MODIFY_FAILED;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_MODIFY_IN_PROGRESS;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_UPDATED_ADDRESS_FAMILY;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_UPDATED_MAX_ENTRIES;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT_WITH_MODIFIED_NAME;
import static software.amazon.ec2.prefixlist.TestHelper.GET_ENTRIES_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.GET_ENTRIES_RESULT;
import static software.amazon.ec2.prefixlist.TestHelper.GET_ENTRIES_RESULT_NO_MODIFICATION;
import static software.amazon.ec2.prefixlist.TestHelper.INVALID_PREFIX_LIST_ID_NOT_FOUND;
import static software.amazon.ec2.prefixlist.TestHelper.INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION;
import static software.amazon.ec2.prefixlist.TestHelper.NOT_UPDATABLE_MESSAGE;
import static software.amazon.ec2.prefixlist.TestHelper.PREFIX_LIST_ID;
import static software.amazon.ec2.prefixlist.TestHelper.PREFIX_LIST_MODIFIED;
import static software.amazon.ec2.prefixlist.TestHelper.PREFIX_LIST_NAME;
import static software.amazon.ec2.prefixlist.TestHelper.PREFIX_LIST_NAME_2;
import static software.amazon.ec2.prefixlist.TestHelper.PREFIX_LIST_WITH_MODIFIED_NAME;
import static software.amazon.ec2.prefixlist.TestHelper.REMOVE_PREFIX_LIST_ENTRY_3;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_HANDLER_REQUEST_WITH_DIFFERENT_PREFIX_LIST_NAME;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_MODEL_MODIFIED;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_MODEL_WITH_DIFFERENT_PREFIX_LIST_NAME;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_MODEL_CREATED;
import static software.amazon.ec2.prefixlist.TestHelper.VERSION_1;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
    }

    @AfterEach
    public void after() {
        verifyNoMoreInteractions(proxy, logger);
    }

    @Test
    public void handleRequestWhenPrefixListNotFound() {
        INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION.setErrorCode(INVALID_PREFIX_LIST_ID_NOT_FOUND);
        doThrow(INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION).when(proxy).injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any());

        Assertions.assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, null, logger);
        });
    }

    @Test
    public void handleRequestWithOnlyTagUpdate() {
        doReturn(DESCRIBE_RESULT).when(proxy).injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST),any());
        doReturn(GET_ENTRIES_RESULT_NO_MODIFICATION).when(proxy).injectCredentialsAndInvoke(eq(GET_ENTRIES_REQUEST),any());
        doReturn(CREATE_TAGS_RESULT).when(proxy).injectCredentialsAndInvoke(eq(CREATE_TAGS_REQUEST),any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_NOT_STARTED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isEqualTo(null);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);

        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);

        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequestWithOnlyNameUpdate() {
        doReturn(DESCRIBE_RESULT_WITH_MODIFIED_NAME).when(proxy).injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST),any());
        doReturn(GET_ENTRIES_RESULT_NO_MODIFICATION).when(proxy).injectCredentialsAndInvoke(eq(GET_ENTRIES_REQUEST),any());

        final ModifyManagedPrefixListRequest modifyRequest = new ModifyManagedPrefixListRequest()
                .withPrefixListName(PREFIX_LIST_NAME_2)
                .withPrefixListId(PREFIX_LIST_ID);

        final ModifyManagedPrefixListResult modifyResult = new ModifyManagedPrefixListResult()
                .withPrefixList(PREFIX_LIST_WITH_MODIFIED_NAME);

        doReturn(modifyResult).when(proxy).injectCredentialsAndInvoke(eq(modifyRequest),any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_DIFFERENT_PREFIX_LIST_NAME, CONTEXT_TAGS_UPDATED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isEqualTo(null);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);

        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_WITH_DIFFERENT_PREFIX_LIST_NAME);

        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequestWithEntryUpdate() {
        doReturn(DESCRIBE_RESULT).when(proxy).injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST),any());
        doReturn(GET_ENTRIES_RESULT).when(proxy).injectCredentialsAndInvoke(eq(GET_ENTRIES_REQUEST),any());


        final ModifyManagedPrefixListRequest modifyRequest = new ModifyManagedPrefixListRequest()
                .withCurrentVersion(VERSION_1)
                .withAddEntries(ImmutableList.of(ADD_PREFIX_LIST_ENTRY_1_MODIFIED, ADD_PREFIX_LIST_ENTRY_2))
                .withRemoveEntries(ImmutableList.of(REMOVE_PREFIX_LIST_ENTRY_3))
                .withPrefixListId(PREFIX_LIST_ID)
                .withPrefixListName(PREFIX_LIST_NAME);
        final ModifyManagedPrefixListResult modifyResult = new ModifyManagedPrefixListResult()
                .withPrefixList(PREFIX_LIST_MODIFIED);

        doReturn(modifyResult).when(proxy).injectCredentialsAndInvoke(eq(modifyRequest),any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_TAGS_UPDATED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED_AND_TAGS_UPDATED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);

        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);

        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        verify(logger).log(any());
    }

    @Test
    public void handlerRequestWithMutationStartedAndModifyInProgressState() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_MODIFY_IN_PROGRESS);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_STARTED_AND_TAGS_UPDATED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED_AND_TAGS_UPDATED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(TestHelper.POLLING_DELAY_SECONDS);

        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);

        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handlerRequestWithMutationStartedAndModifyFailedState() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_MODIFY_FAILED);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_STARTED_AND_TAGS_UPDATED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualTo(CONTEXT_MUTATION_STARTED_AND_TAGS_UPDATED);

        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_CREATED);

        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handlerRequestWithMutationStartedAndModifyCompleteState() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_MODIFY_COMPLETE);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_STARTED_AND_TAGS_UPDATED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);

        assertThat(response.getResourceModel()).isEqualTo(RESOURCE_MODEL_MODIFIED);

        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handlerRequestWithUpdateOnMaxEntries() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_UPDATED_MAX_ENTRIES);
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_NOT_STARTED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo(NOT_UPDATABLE_MESSAGE);
    }

    @Test
    public void handlerRequestWithUpdateOnAddressFamily() {
        when(proxy.injectCredentialsAndInvoke(eq(DESCRIBE_REQUEST), any())).thenReturn(DESCRIBE_RESULT_UPDATED_ADDRESS_FAMILY);
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, CONTEXT_MUTATION_NOT_STARTED, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo(NOT_UPDATABLE_MESSAGE);
    }
}
