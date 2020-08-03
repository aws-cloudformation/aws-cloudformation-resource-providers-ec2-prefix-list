package software.amazon.ec2.prefixlist;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CallbackContext {

    @Builder.Default
    private boolean mutationStarted = false;

    @Builder.Default
    private boolean tagsUpdated = false;

    private String prefixListId;
}
