package com.example.projectCollab.insights;

import com.example.projectCollab.entity.Milestone;
import com.example.projectCollab.entity.Task;
import com.example.projectCollab.entity.Team;

import java.util.ArrayList;
import java.util.List;

public class TeamRiskAssessment {

    public static final String HEALTHY = "HEALTHY";
    public static final String WATCH = "WATCH";
    public static final String AT_RISK = "AT_RISK";

    private Team team;
    private int score;
    private String band;
    private List<String> reasons = new ArrayList<>();
    private long taskTotal;
    private long taskCompleted;
    private long overdueTaskCount;
    private long dueSoonTaskCount;
    private long overdueMilestoneCount;
    private long dueSoonMilestoneCount;
    private List<Task> overdueTasks = new ArrayList<>();
    private List<Task> dueSoonTasks = new ArrayList<>();
    private List<Milestone> overdueMilestones = new ArrayList<>();
    private List<Milestone> dueSoonMilestones = new ArrayList<>();

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getBand() {
        return band;
    }

    public void setBand(String band) {
        this.band = band;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public long getTaskTotal() {
        return taskTotal;
    }

    public void setTaskTotal(long taskTotal) {
        this.taskTotal = taskTotal;
    }

    public long getTaskCompleted() {
        return taskCompleted;
    }

    public void setTaskCompleted(long taskCompleted) {
        this.taskCompleted = taskCompleted;
    }

    public long getOverdueTaskCount() {
        return overdueTaskCount;
    }

    public void setOverdueTaskCount(long overdueTaskCount) {
        this.overdueTaskCount = overdueTaskCount;
    }

    public long getDueSoonTaskCount() {
        return dueSoonTaskCount;
    }

    public void setDueSoonTaskCount(long dueSoonTaskCount) {
        this.dueSoonTaskCount = dueSoonTaskCount;
    }

    public long getOverdueMilestoneCount() {
        return overdueMilestoneCount;
    }

    public void setOverdueMilestoneCount(long overdueMilestoneCount) {
        this.overdueMilestoneCount = overdueMilestoneCount;
    }

    public long getDueSoonMilestoneCount() {
        return dueSoonMilestoneCount;
    }

    public void setDueSoonMilestoneCount(long dueSoonMilestoneCount) {
        this.dueSoonMilestoneCount = dueSoonMilestoneCount;
    }

    public List<Task> getOverdueTasks() {
        return overdueTasks;
    }

    public void setOverdueTasks(List<Task> overdueTasks) {
        this.overdueTasks = overdueTasks;
    }

    public List<Task> getDueSoonTasks() {
        return dueSoonTasks;
    }

    public void setDueSoonTasks(List<Task> dueSoonTasks) {
        this.dueSoonTasks = dueSoonTasks;
    }

    public List<Milestone> getOverdueMilestones() {
        return overdueMilestones;
    }

    public void setOverdueMilestones(List<Milestone> overdueMilestones) {
        this.overdueMilestones = overdueMilestones;
    }

    public List<Milestone> getDueSoonMilestones() {
        return dueSoonMilestones;
    }

    public void setDueSoonMilestones(List<Milestone> dueSoonMilestones) {
        this.dueSoonMilestones = dueSoonMilestones;
    }

    public boolean isAtRisk() {
        return AT_RISK.equals(band);
    }
}
