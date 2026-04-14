package bachelor.code.web;

import bachelor.code.entity.AuditLog;
import bachelor.code.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/audit")
public class WebAuditController {

    private final AuditLogService auditLogService;

    public WebAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String auditLog(Model model) {
        List<AuditLog> logs = auditLogService.getAll();
        model.addAttribute("logs", logs);
        return "admin/audit";
    }
}
