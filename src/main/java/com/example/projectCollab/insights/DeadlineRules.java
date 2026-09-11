package com.example.projectCollab.insights;

import com.example.projectCollab.entity.Milestone;
import com.example.projectCollab.entity.Task;
import com.example.projectCollab.entity.TaskStatus;

import java.time.LocalDateTime;

public final class DeadlineRules {

    public static final int DUE_SOON_HOURS = 48;

    private DeadlineRules() {
    }

    public static boolean isTaskIncomplete(Task task) {
        return task != null && task.getStatus() != TaskStatus.COMPLETED;
    }

    public static boolean isTaskOverdue(Task task, LocalDateTime now) {
        return isTaskIncomplete(task)
                && task.getDeadline() != null
                && task.getDeadline().isBefore(now);
    }

    public static boolean isTaskDueSoon(Task task, LocalDateTime now) {
        if (!isTaskIncomplete(task) || task.getDeadline() == null) {
            return false;
        }
        LocalDateTime until = now.plusHours(DUE_SOON_HOURS);
        return !task.getDeadline().isBefore(now) && !task.getDeadline().isAfter(until);
    }

    public static boolean isMilestoneIncomplete(Milestone milestone) {
        return milestone != null && !Boolean.TRUE.equals(milestone.getIsCompleted());
    }

    public static boolean isMilestoneOverdue(Milestone milestone, LocalDateTime now) {
        return isMilestoneIncomplete(milestone)
                && milestone.getDeadline() != null
                && milestone.getDeadline().isBefore(now);
    }

    public static boolean isMilestoneDueSoon(Milestone milestone, LocalDateTime now) {
        if (!isMilestoneIncomplete(milestone) || milestone.getDeadline() == null) {
            return false;
        }
        LocalDateTime until = now.plusHours(DUE_SOON_HOURS);
        return !milestone.getDeadline().isBefore(now) && !milestone.getDeadline().isAfter(until);
    }
}
