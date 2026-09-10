package com.example.projectCollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportSection {
    private String title;
    private List<String> columns = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
}
