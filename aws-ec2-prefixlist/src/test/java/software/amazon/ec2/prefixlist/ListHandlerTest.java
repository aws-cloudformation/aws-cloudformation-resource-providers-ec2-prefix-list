package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsRequest;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static software.amazon.ec2.prefixlist.TestHelper.DESCRIBE_RESULT;
import static software.amazon.ec2.prefixlist.TestHelper.GET_ENTRIES_REQUEST;
import static software.amazon.ec2.prefixlist.TestHelper.GET_ENTRIES_RESULT;
import static software.amazon.ec2.prefixlist.TestHelper.PREFIX_LIST;
import static software.amazon.ec2.prefixlist.TestHelper.PREFIX_LIST_ENTRIES;
import static software.amazon.ec2.prefixlist.TestHelper.RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID;
import static software.amazon.ec2.prefixlist.TestHelper.convertToEntries;
import static software.amazon.ec2.prefixlist.TestHelper.convertToResourceModelTags;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
    }

    @AfterEach
    public void after() {
        verifyNoMoreInteractions(proxy, logger);
    }

    @Test
    public void handleRequestSimpleSuccess() {
        doReturn(DESCRIBE_RESULT).when(proxy).injectCredentialsAndInvoke(eq(new DescribeManagedPrefixListsRequest()),any());
        doReturn(GET_ENTRIES_RESULT).when(proxy).injectCredentialsAndInvoke(eq(GET_ENTRIES_REQUEST),any());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID, null, logger);

        final List<ResourceModel> expectedResourceModels = ImmutableList.of(ResourceModel.builder()
                .prefixListId(PREFIX_LIST.getPrefixListId())
                .prefixListName(PREFIX_LIST.getPrefixListName())
                .addressFamily(PREFIX_LIST.getAddressFamily())
                .maxEntries(PREFIX_LIST.getMaxEntries())
                .version(PREFIX_LIST.getVersion().intValue())
                .ownerId(PREFIX_LIST.getOwnerId())
                .arn(PREFIX_LIST.getPrefixListArn())
                .tags(convertToResourceModelTags(PREFIX_LIST.getTags()))
                .entries(convertToEntries(PREFIX_LIST_ENTRIES))
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels().equals(expectedResourceModels));
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
