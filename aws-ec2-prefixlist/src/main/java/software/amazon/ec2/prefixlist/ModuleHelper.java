package software.amazon.ec2.prefixlist;

import com.amazonaws.services.ec2.model.AddPrefixListEntry;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.PrefixListEntry;
import com.amazonaws.services.ec2.model.RemovePrefixListEntry;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.google.common.collect.ImmutableList;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.amazonaws.util.CollectionUtils.isNullOrEmpty;

class ModuleHelper {
    static final String CREATE_IN_PROGRESS = "create-in-progress";
    static final String MODIFY_IN_PROGRESS = "modify-in-progress";
    static final String CREATE_FAILED = "create-failed";
    static final String MODIFY_FAILED = "modify-failed";
    static final String DELETE_FAILED = "delete-failed";
    static final String DELETE_IN_PROGRESS = "delete-in-progress";
    static final Integer POLLING_DELAY_SECONDS = 5;
    static final String INVALID_PREFIX_LIST_ID_NOT_FOUND = "InvalidPrefixListID.NotFound";
    static final String PREFIX_LIST_RESOURCE = "prefix-list";

    static List<AddPrefixListEntry> convertToAddPrefixListEntries(final List<Entry> entries) {
        if (entries == null) {
            return ImmutableList.of();
        } else {
             return entries.stream()
                    .map(entry -> new AddPrefixListEntry()
                            .withCidr(entry.getCidr())
                            .withDescription(entry.getDescription()))
                    .collect(Collectors.toList());
        }
    }
    static List<RemovePrefixListEntry> convertToRemovePrefixListEntries(final List<Entry> entries) {
        if (entries == null) {
            return ImmutableList.of();
        } else {
            return entries.stream()
                    .map(entry -> new RemovePrefixListEntry()
                            .withCidr(entry.getCidr()))
                    .collect(Collectors.toList());
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
    static List<TagSpecification> convertToTagSpecifications(final List<Tag> tags) {
        if (isNullOrEmpty(tags)) {
            return ImmutableList.of();
        } else {
            return ImmutableList.of(new TagSpecification()
                    .withResourceType(PREFIX_LIST_RESOURCE)
                    .withTags(convertToEc2Tags(tags)));
        }
    }

    static List<com.amazonaws.services.ec2.model.Tag> convertToEc2Tags(final List<Tag> tags) {
        if (tags == null) {
            return ImmutableList.of();
        } else {
            List<com.amazonaws.services.ec2.model.Tag> ec2Tags = tags.stream()
                    .map(tag -> new com.amazonaws.services.ec2.model.Tag()
                            .withKey(tag.getKey())
                            .withValue(tag.getValue()))
                    .collect(Collectors.toList());
            return ec2Tags;
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

    /*
     * This method will convert catched exception from the request
     */
    static <T> T invokeAndConvertException(final Supplier<T> supplier, String prefixListId) {
        try {
            return supplier.get();
        } catch (final AmazonEC2Exception ex) {
            if(ex.getErrorCode().equals(INVALID_PREFIX_LIST_ID_NOT_FOUND)) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, prefixListId);
            }
            throw ex;
        }
    }

}
