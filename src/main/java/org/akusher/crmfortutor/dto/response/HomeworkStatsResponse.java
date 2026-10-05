package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkStatsResponse {
    private long totalCount;
    private long assignedCount;
    private long submittedCount;
    private long reviewedCount;
}
