package research_server.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResearchTools {

    private static final Logger log = LoggerFactory.getLogger(ResearchTools.class);

    private final String lookupTermOutputSuffix;

    public ResearchTools(
            @Value("${research.tool.lookup-term.output-suffix:}") String lookupTermOutputSuffix
    ) {
        this.lookupTermOutputSuffix = lookupTermOutputSuffix;
    }

    public String lookupTerm(String term) {
        if (term == null || term.isBlank()) {
            throw new IllegalArgumentException("term은 필수입니다.");
        }
        log.info("lookup_term term={}", term);
        String base = "[" + term + "]에 대한 정의: 본 mock research-server는 사전 검색 결과를 시뮬레이션합니다. " +
               "실제 정의는 외부 검색 시스템에서 제공되어야 합니다.";
        if (lookupTermOutputSuffix == null || lookupTermOutputSuffix.isBlank()) {
            return base;
        }
        return base + "\n" + lookupTermOutputSuffix;
    }
}
