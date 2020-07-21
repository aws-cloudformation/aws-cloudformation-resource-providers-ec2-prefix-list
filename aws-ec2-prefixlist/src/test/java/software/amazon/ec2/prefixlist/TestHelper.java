package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.model.AddPrefixListEntry;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateTagsResult;
import com.amazonaws.services.ec2.model.DeleteManagedPrefixListRequest;
import com.amazonaws.services.ec2.model.DeleteManagedPrefixListResult;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsResult;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsRequest;
import com.amazonaws.services.ec2.model.DescribeManagedPrefixListsResult;
import com.amazonaws.services.ec2.model.GetManagedPrefixListEntriesRequest;
import com.amazonaws.services.ec2.model.GetManagedPrefixListEntriesResult;
import com.amazonaws.services.ec2.model.ManagedPrefixList;
import com.amazonaws.services.ec2.model.PrefixListEntry;
import com.amazonaws.services.ec2.model.RemovePrefixListEntry;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.google.common.collect.ImmutableList;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

class TestHelper {
    static final String ADDRESS_FAMILY_4 = "IPv4";
    static final String ADDRESS_FAMILY_6 = "IPv6";
    static final String PREFIX_LIST_NAME = "pl-name";
    static final String PREFIX_LIST_NAME_2 = "pl-name_2";
    static final String CIDR_1 = "1.1.1.1/32";
    static final String CIDR_2 = "1.1.1.2/32";
    static final String CIDR_3 = "1.1.1.3/32";
    static final String DESCRIPTION_1 = "Some description here";
    static final String DESCRIPTION_2 = "Some other description here";
    static final long VERSION_1 = 1;
    static final long VERSION_2 = 2;
    static final String PREFIX_LIST_ID = "pl-123456";
    static final Integer POLLING_DELAY_SECONDS = 5;
    static final String INVALID_PREFIX_LIST_ID_NOT_FOUND = "InvalidPrefixListID.NotFound";
    static final String CREATE_IN_PROGRESS = "create-in-progress";
    static final String CREATE_COMPLETE = "create-complete";
    static final String MODIFY_COMPLETE = "modify-complete";
    static final String MODIFY_IN_PROGRESS = "modify-in-progress";
    static final String DELETE_IN_PROGRESS = "delete-in-progress";
    static final String CREATE_FAILED = "create-failed";
    static final String MODIFY_FAILED = "modify-failed";
    static final String DELETE_FAILED = "delete-failed";
    static final Integer MAX_ENTRIES_5 = 5;
    static final Integer MAX_ENTRIES_6 = 6;
    static final String OWNER_ID = "123456789";
    static final String PREFIX_LIST_ARN = "arn:aws:ec2:us-east-1:" + OWNER_ID + ":prefix-list/" + PREFIX_LIST_ID;
    static final String NOT_UPDATABLE_MESSAGE = "MaxEntries or AddressFamily is not updatable.";

    //Entries
    static final Entry ENTRY_1 = Entry.builder()
            .cidr(CIDR_1)
            .description(DESCRIPTION_1)
            .build();
    static final Entry ENTRY_1_MODIFIED = Entry.builder()
            .cidr(CIDR_1)
            .description(DESCRIPTION_2)
            .build();
    static final Entry ENTRY_2 = Entry.builder()
            .cidr(CIDR_2)
            .description(DESCRIPTION_1)
            .build();
    static final Entry ENTRY_3 = Entry.builder()
            .cidr(CIDR_3)
            .description(DESCRIPTION_1)
            .build();

    static final PrefixListEntry PREFIX_LIST_ENTRY_1 = new PrefixListEntry()
            .withCidr(CIDR_1)
            .withDescription(DESCRIPTION_1);
    static final PrefixListEntry PREFIX_LIST_ENTRY_1_MODIFIED = new PrefixListEntry()
            .withCidr(CIDR_1)
            .withDescription(DESCRIPTION_2);
    static final PrefixListEntry PREFIX_LIST_ENTRY_2 = new PrefixListEntry()
            .withCidr(CIDR_2)
            .withDescription(DESCRIPTION_1);
    static final PrefixListEntry PREFIX_LIST_ENTRY_3 = new PrefixListEntry()
            .withCidr(CIDR_3)
            .withDescription(DESCRIPTION_1);

    static final AddPrefixListEntry ADD_PREFIX_LIST_ENTRY_1_MODIFIED = new AddPrefixListEntry()
            .withCidr(CIDR_1)
            .withDescription(DESCRIPTION_2);
    static final AddPrefixListEntry ADD_PREFIX_LIST_ENTRY_2 = new AddPrefixListEntry()
            .withCidr(CIDR_2)
            .withDescription(DESCRIPTION_1);

    static final RemovePrefixListEntry REMOVE_PREFIX_LIST_ENTRY_3 = new RemovePrefixListEntry()
            .withCidr(CIDR_3);

    static final List<Entry> ENTRIES = ImmutableList.of(ENTRY_1_MODIFIED, ENTRY_2);
    static final List<AddPrefixListEntry> ADD_ENTRIES = ImmutableList.of(ADD_PREFIX_LIST_ENTRY_1_MODIFIED, ADD_PREFIX_LIST_ENTRY_2);
    static final List<PrefixListEntry> PREFIX_LIST_ENTRIES = ImmutableList.of(PREFIX_LIST_ENTRY_1, PREFIX_LIST_ENTRY_3);
    static final List<PrefixListEntry> PREFIX_LIST_ENTRIES_NO_MODIFICATION = ImmutableList.of(PREFIX_LIST_ENTRY_1_MODIFIED, PREFIX_LIST_ENTRY_2);

    // Tagging
    static final String TAG_KEY_1 = "Purpose";
    static final String TAG_VALUE = "Testing";
    static final String PREFIX_LIST_RESOURCE_TYPE = "prefix-list";
    static final Tag TAG_1 = Tag.builder()
            .key(TAG_KEY_1)
            .value(TAG_VALUE)
            .build();
    static final com.amazonaws.services.ec2.model.Tag EC2TAG = new com.amazonaws.services.ec2.model.Tag()
            .withKey(TAG_KEY_1)
            .withValue(TAG_VALUE);
    static final List<Tag> TAG_LIST_1 = ImmutableList.of(TAG_1);
    static final List<com.amazonaws.services.ec2.model.Tag> EC2_TAG_LIST =  ImmutableList.of(EC2TAG);

    // Resource Model and Resource Handlers
    static final ResourceModel RESOURCE_MODEL = ResourceModel.builder()
            .maxEntries(MAX_ENTRIES_5)
            .addressFamily(ADDRESS_FAMILY_4)
            .prefixListName(PREFIX_LIST_NAME)
            .entries(ENTRIES)
            .tags(TAG_LIST_1)
            .build();

    static final ResourceModel RESOURCE_MODEL_CREATED = ResourceModel.builder()
            .maxEntries(MAX_ENTRIES_5)
            .addressFamily(ADDRESS_FAMILY_4)
            .prefixListName(PREFIX_LIST_NAME)
            .entries(ENTRIES)
            .tags(TAG_LIST_1)
            .prefixListId(PREFIX_LIST_ID)
            .version(((int) VERSION_1))
            .ownerId(OWNER_ID)
            .arn(PREFIX_LIST_ARN)
            .build();

    static final ResourceModel RESOURCE_MODEL_MODIFIED = ResourceModel.builder()
            .maxEntries(MAX_ENTRIES_5)
            .addressFamily(ADDRESS_FAMILY_4)
            .prefixListName(PREFIX_LIST_NAME)
            .entries(ENTRIES)
            .tags(TAG_LIST_1)
            .prefixListId(PREFIX_LIST_ID)
            .version(((int) VERSION_2))
            .ownerId(OWNER_ID)
            .arn(PREFIX_LIST_ARN)
            .build();

    static final ResourceModel RESOURCE_MODEL_WITH_DIFFERENT_PREFIX_LIST_NAME = ResourceModel.builder()
            .maxEntries(MAX_ENTRIES_5)
            .addressFamily(ADDRESS_FAMILY_4)
            .prefixListName(PREFIX_LIST_NAME_2)
            .entries(ENTRIES)
            .tags(TAG_LIST_1)
            .prefixListId(PREFIX_LIST_ID)
            .version(((int) VERSION_2))
            .ownerId(OWNER_ID)
            .arn(PREFIX_LIST_ARN)
            .build();

    static final ResourceHandlerRequest<ResourceModel> RESOURCE_HANDLER_REQUEST = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(RESOURCE_MODEL)
            .build();

    static final ResourceHandlerRequest<ResourceModel> RESOURCE_HANDLER_REQUEST_WITH_PREFIX_LIST_ID = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(RESOURCE_MODEL_CREATED)
            .build();

    static final ResourceHandlerRequest<ResourceModel> RESOURCE_HANDLER_REQUEST_WITH_DIFFERENT_PREFIX_LIST_NAME = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(RESOURCE_MODEL_WITH_DIFFERENT_PREFIX_LIST_NAME)
            .build();

    // Prefix lists
    static final ManagedPrefixList PREFIX_LIST = new ManagedPrefixList()
            .withPrefixListName(PREFIX_LIST_NAME)
            .withAddressFamily(ADDRESS_FAMILY_4)
            .withMaxEntries(MAX_ENTRIES_5)
            .withVersion(VERSION_1)
            .withPrefixListId(PREFIX_LIST_ID)
            .withPrefixListArn(PREFIX_LIST_ARN)
            .withOwnerId(OWNER_ID);

    static final ManagedPrefixList PREFIX_LIST_WITH_UPDATED_MAX_ENTRIES = new ManagedPrefixList()
            .withPrefixListName(PREFIX_LIST_NAME)
            .withAddressFamily(ADDRESS_FAMILY_4)
            .withMaxEntries(MAX_ENTRIES_6)
            .withVersion(VERSION_1)
            .withPrefixListId(PREFIX_LIST_ID)
            .withPrefixListArn(PREFIX_LIST_ARN)
            .withOwnerId(OWNER_ID);

    static final ManagedPrefixList PREFIX_LIST_WITH_UPDATED_ADDRESS_FAMILY = new ManagedPrefixList()
            .withPrefixListName(PREFIX_LIST_NAME)
            .withAddressFamily(ADDRESS_FAMILY_6)
            .withMaxEntries(MAX_ENTRIES_6)
            .withVersion(VERSION_1)
            .withPrefixListId(PREFIX_LIST_ID)
            .withPrefixListArn(PREFIX_LIST_ARN)
            .withOwnerId(OWNER_ID);

    static final ManagedPrefixList PREFIX_LIST_WITH_MODIFIED_NAME = PREFIX_LIST.clone().withPrefixListName(PREFIX_LIST_NAME);

    static final ManagedPrefixList PREFIX_LIST_MODIFIED = PREFIX_LIST.clone().withVersion(VERSION_2);

    static final ManagedPrefixList PREFIX_LIST_CREATE_COMPLETE = PREFIX_LIST.clone().withState(CREATE_COMPLETE);

    static final ManagedPrefixList PREFIX_LIST_CREATE_IN_PROGRESS = PREFIX_LIST.clone().withState(CREATE_IN_PROGRESS);

    static final ManagedPrefixList PREFIX_LIST_CREATE_FAILED = PREFIX_LIST.clone().withState(CREATE_FAILED);

    static final ManagedPrefixList PREFIX_LIST_MODIFY_IN_PROGRESS= PREFIX_LIST.clone().withState(MODIFY_IN_PROGRESS);

    static final ManagedPrefixList PREFIX_LIST_MODIFY_FAILED= PREFIX_LIST.clone().withState(MODIFY_FAILED);

    static final ManagedPrefixList PREFIX_LIST_MODIFY_COMPLETE = PREFIX_LIST_MODIFIED.clone().withState(MODIFY_COMPLETE);

    static final ManagedPrefixList PREFIX_LIST_DELETE_IN_PROGRESS = PREFIX_LIST.clone().withState(DELETE_IN_PROGRESS);

    static final ManagedPrefixList PREFIX_LIST_DELETE_FAILED = PREFIX_LIST.clone().withState(DELETE_FAILED);

    static final ManagedPrefixList PREFIX_LIST_WITH_TAGS = PREFIX_LIST.clone().withTags(EC2_TAG_LIST);


    // DescribeManagedPrefixLists Requests and Results
    static final DescribeManagedPrefixListsRequest DESCRIBE_REQUEST = new DescribeManagedPrefixListsRequest()
            .withPrefixListIds(ImmutableList.of(PREFIX_LIST_ID));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_MODIFY_COMPLETE = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_MODIFY_COMPLETE));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_WITH_TAGS = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_WITH_TAGS));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_WITH_MODIFIED_NAME = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_WITH_MODIFIED_NAME));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_CREATE_COMPLETE = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_CREATE_COMPLETE));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_CREATE_IN_PROGRESS = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_CREATE_IN_PROGRESS));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_CREATE_FAILED = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_CREATE_FAILED));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_MODIFY_IN_PROGRESS = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_MODIFY_IN_PROGRESS));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_MODIFY_FAILED = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_MODIFY_FAILED));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_DELETE_IN_PROGRESS = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_DELETE_IN_PROGRESS));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_DELETE_FAILED = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_DELETE_FAILED));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_UPDATED_MAX_ENTRIES = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_WITH_UPDATED_MAX_ENTRIES));

    static final DescribeManagedPrefixListsResult DESCRIBE_RESULT_UPDATED_ADDRESS_FAMILY = new DescribeManagedPrefixListsResult()
            .withPrefixLists(ImmutableList.of(PREFIX_LIST_WITH_UPDATED_ADDRESS_FAMILY));

    // GetManagedPrefixList Requests and Results
    static final GetManagedPrefixListEntriesRequest GET_ENTRIES_REQUEST = new GetManagedPrefixListEntriesRequest()
            .withPrefixListId(PREFIX_LIST_ID);

    static final GetManagedPrefixListEntriesResult GET_ENTRIES_RESULT = new GetManagedPrefixListEntriesResult()
            .withEntries(PREFIX_LIST_ENTRIES);

    static final GetManagedPrefixListEntriesResult GET_ENTRIES_RESULT_NO_MODIFICATION = new GetManagedPrefixListEntriesResult()
            .withEntries(PREFIX_LIST_ENTRIES_NO_MODIFICATION);

    // DeleteManagedPrefixList Requests and Results
    static final DeleteManagedPrefixListRequest DELETE_REQUEST =  new DeleteManagedPrefixListRequest()
            .withPrefixListId(PREFIX_LIST_ID);

    static final DeleteManagedPrefixListResult DELETE_RESULT = new DeleteManagedPrefixListResult()
            .withPrefixList(PREFIX_LIST);

    // EC2 Tagging service Request and Results
    static final CreateTagsRequest CREATE_TAGS_REQUEST = new CreateTagsRequest()
            .withResources(PREFIX_LIST_ID)
            .withTags(EC2_TAG_LIST);

    static final CreateTagsResult CREATE_TAGS_RESULT = new CreateTagsResult();

    static final DeleteTagsRequest DELETE_TAGS_REQUEST = new DeleteTagsRequest()
            .withResources(PREFIX_LIST_ID)
            .withTags(ImmutableList.of());
    static final DeleteTagsResult DELETE_TAGS_RESULT = new DeleteTagsResult();

    // CallbackContexts
    static final CallbackContext CONTEXT_MUTATION_NOT_STARTED = CallbackContext.builder().build();

    static final CallbackContext CONTEXT_TAGS_UPDATED = CONTEXT_MUTATION_NOT_STARTED.toBuilder()
            .tagsUpdated(true)
            .build();

    static final CallbackContext CONTEXT_MUTATION_STARTED = CONTEXT_MUTATION_NOT_STARTED.toBuilder()
            .mutationStarted(true)
            .build();

    static final CallbackContext CONTEXT_MUTATION_STARTED_AND_TAGS_UPDATED = CONTEXT_MUTATION_STARTED.toBuilder()
            .tagsUpdated(true)
            .build();

    static final CallbackContext CONTEXT_MUTATION_STARTED_WITH_PREFIX_LIST_ID = CONTEXT_MUTATION_STARTED.toBuilder()
            .prefixListId(PREFIX_LIST_ID)
            .build();

    // EC2 Exception
    static final AmazonEC2Exception INVALID_PREFIX_LIST_ID_NOT_FOUND_EXCEPTION = new AmazonEC2Exception("Id Not Found");

    static List<TagSpecification> convertToTagSpecifications(final List<Tag> tags) {
        if (tags == null) {
            return ImmutableList.of();
        } else {
            List<com.amazonaws.services.ec2.model.Tag> ec2Tags = tags.stream()
                    .map(tag -> new com.amazonaws.services.ec2.model.Tag()
                            .withKey(tag.getKey())
                            .withValue(tag.getValue()))
                    .collect(Collectors.toList());
            return ImmutableList.of(new TagSpecification()
                    .withResourceType(PREFIX_LIST_RESOURCE_TYPE)
                    .withTags(ec2Tags));
        }
    }

    static List<Entry> convertToEntries(final List<PrefixListEntry> prefixListEntries) {
        if (prefixListEntries == null) {
            return ImmutableList.of();
        } else {
            return prefixListEntries.stream()
                    .map(prefixListEntry -> Entry.builder()
                            .cidr(prefixListEntry.getCidr())
                            .description(prefixListEntry.getDescription())
                            .build())
                    .collect(Collectors.toList());
        }
    }


    static List<Tag> convertToResourceModelTags(final List<com.amazonaws.services.ec2.model.Tag> tags) {
        if (tags == null) {
            return ImmutableList.of();
        } else {
            List<Tag> resourceModelTags = tags.stream()
                    .map(tag -> Tag.builder()
                            .key(tag.getKey())
                            .value(tag.getValue())
                            .build())
                    .collect(Collectors.toList());
            return resourceModelTags;
        }
    }
}
