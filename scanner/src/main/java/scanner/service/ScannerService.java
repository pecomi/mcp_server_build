package scanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import scanner.dto.Finding;
import scanner.dto.McpTool;
import scanner.dto.ScanResponse;
import scanner.rules.Rule;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScannerService {

    private static final Logger log = LoggerFactory.getLogger(ScannerService.class);

    private final McpToolFetcher fetcher;
    private final List<Rule> rules;

    public ScannerService(McpToolFetcher fetcher, List<Rule> rules) {
        this.fetcher = fetcher;
        this.rules = rules;
    }

    public ScanResponse scan(String targetUrl, String apiKey) {
        log.info("scan target={} rules={}", targetUrl,
                rules.stream().map(Rule::id).toList());

        List<McpTool> tools = fetcher.fetch(targetUrl, apiKey);
        List<Finding> findings = new ArrayList<>();
        for (McpTool tool : tools) {
            for (Rule rule : rules) {
                findings.addAll(rule.apply(tool));
            }
        }

        log.info("scan complete: {} tools, {} findings", tools.size(), findings.size());
        return new ScanResponse(targetUrl, tools.size(), findings);
    }
}
