package bachelor.code.config;

import bachelor.code.dto.CreateAccountingDocumentDto;
import bachelor.code.dto.CreateApprovalRequestDto;
import bachelor.code.entity.AccountingDocument;
import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.User;
import bachelor.code.enums.DocumentType;
import bachelor.code.enums.RequestType;
import bachelor.code.repository.AccountingDocumentRepository;
import bachelor.code.repository.ApprovalRequestRepository;
import bachelor.code.repository.UserRepository;
import bachelor.code.service.AccountingDocumentService;
import bachelor.code.service.ApprovalRequestService;
import bachelor.code.service.ApprovalWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the database with realistic demo data — requests in various states,
 * accounting documents, audit log entries. Idempotent: runs only if there
 * are no approval_requests yet.
 *
 * Controlled via env var APP_DEMO_DATA=true (default: false).
 * Run once via Railway: set APP_DEMO_DATA=true, restart, then turn it off.
 */
@Component
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final ApprovalRequestRepository requestRepository;
    private final AccountingDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ApprovalRequestService requestService;
    private final ApprovalWorkflowService workflowService;
    private final AccountingDocumentService documentService;

    private final boolean enabled;

    public DemoDataInitializer(ApprovalRequestRepository requestRepository,
                               AccountingDocumentRepository documentRepository,
                               UserRepository userRepository,
                               ApprovalRequestService requestService,
                               ApprovalWorkflowService workflowService,
                               AccountingDocumentService documentService,
                               @Value("${app.demo-data:false}") boolean enabled) {
        this.requestRepository = requestRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.requestService = requestService;
        this.workflowService = workflowService;
        this.documentService = documentService;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedIfEnabled() {
        if (!enabled) {
            return;
        }
        if (requestRepository.count() > 0) {
            log.info("Demo data: skipping — approval_requests already populated.");
            return;
        }

        log.info("Demo data: seeding…");

        // Load some seeded users (from DataInitializer)
        User admin = userRepository.findByEmail("admin@company.com").orElseThrow();
        User matera = userRepository.findByEmail("matera@medicton.com").orElseThrow();
        User fabian = userRepository.findByEmail("fabian@medicton.com").orElseThrow();
        // Requesters (non-approvers, to keep flow clean)
        List<User> requesters = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(admin.getId())
                          && !u.getId().equals(matera.getId())
                          && !u.getId().equals(fabian.getId())
                          && u.isPasswordSet())
                .toList();

        User r1 = requesters.isEmpty() ? admin : requesters.get(0);
        User r2 = requesters.size() < 2 ? r1 : requesters.get(1);
        User r3 = requesters.size() < 3 ? r1 : requesters.get(2);

        // ── Approval requests in various states ──────────────────────

        // 1. Draft (NEW) — small expense
        createDraft(r1, "Kancelářské potřeby Q2", RequestType.EXPENSE,
                new BigDecimal("1200.00"), "Papírnictví Petr", "IT",
                "Papír A4, tonery, propisky", "Doplnění zásob na kancelář");

        // 2. Submitted PURCHASE — pending Matera (step 1)
        Long id2 = createDraft(r2, "Notebook ThinkPad T14", RequestType.PURCHASE,
                new BigDecimal("28500.00"), "Alza.cz", "IT",
                "Lenovo ThinkPad T14 Gen 4, i7, 16GB RAM, 512GB SSD",
                "Náhrada zastaralého HW pro vývojářku");
        requestService.submit(id2, r2);

        // 3. Submitted EXPENSE L1 — pending APPROVER role
        Long id3 = createDraft(r1, "Občerstvení na poradu", RequestType.EXPENSE,
                new BigDecimal("850.00"), "Globus", "management",
                "Káva, voda, sušenky pro zákaznický meeting", "Hostit dodavatele");
        requestService.submit(id3, r1);

        // 4. Submitted + approved — small expense, fully approved
        Long id4 = createDraft(r3, "Roční licence Antivirus", RequestType.EXPENSE,
                new BigDecimal("2400.00"), "ESET ČR", "IT",
                "ESET Endpoint Protection — obnova ročního předplatného",
                "Zachování bezpečnosti firemních počítačů");
        requestService.submit(id4, r3);
        approveIfPossible(id4, "Obnova schválena, prodloužení licence v pořádku.");

        // 5. Rejected EXPENSE
        Long id5 = createDraft(r1, "Konference Web Summit Lisabon", RequestType.EXPENSE,
                new BigDecimal("45000.00"), "Web Summit", "management",
                "Účast 2 zaměstnanců na konferenci včetně letenek a ubytování",
                "Networking a prezentace firmy");
        requestService.submit(id5, r1);
        rejectIfPossible(id5, "Rozpočet již vyčerpán, přesouváme do Q4.");

        // 6. Returned for revision
        Long id6 = createDraft(r2, "Marketing kampaň LinkedIn", RequestType.EXPENSE,
                new BigDecimal("8000.00"), "LinkedIn Ads", "management",
                "Propagace nové služby", "Získání nových B2B leadů");
        requestService.submit(id6, r2);
        returnIfPossible(id6, "Chybí konkrétní cíle kampaně a měřitelné KPI.");

        // 7. Big PURCHASE — pending step 1 (Matera)
        Long id7 = createDraft(r3, "Konferenční stůl pro zasedačku", RequestType.PURCHASE,
                new BigDecimal("18500.00"), "IKEA Business", "management",
                "Velký stůl pro 12 osob, deska dub", "Vybavení nové zasedací místnosti");
        requestService.submit(id7, r3);

        // 8. PURCHASE — passed step 1, pending step 2 (Fabián)
        Long id8 = createDraft(r1, "Server pro vývojové prostředí", RequestType.PURCHASE,
                new BigDecimal("65000.00"), "CZC.cz", "IT",
                "Dell PowerEdge T350, Xeon, 64GB RAM",
                "Lokální dev/test prostředí pro tým");
        requestService.submit(id8, r1);
        approveIfPossible(id8, "HW specifikace v pořádku, doporučuji ke schválení.");

        // 9. Mid-range EXPENSE — pending finanční ředitel
        Long id9 = createDraft(r2, "Školení Spring Boot pro tým", RequestType.EXPENSE,
                new BigDecimal("18000.00"), "ITNetwork", "IT",
                "Online kurz pro 3 vývojáře", "Zvýšení kvalifikace");
        requestService.submit(id9, r2);

        // 10. Another draft
        createDraft(r3, "Nový kávovar do kuchyňky", RequestType.EXPENSE,
                new BigDecimal("4200.00"), "DeLonghi", "management",
                "Automatický kávovar s mlýnkem", "Náhrada za rozbitý starý");

        log.info("Demo data: created 10 approval requests in various states.");

        // ── Accounting documents ──────────────────────────────────────

        // Doc 1: Draft
        createDoc(r1, DocumentType.INVOICE, "Alza.cz", "27082440",
                new BigDecimal("2800.00"), new BigDecimal("3388.00"),
                "Externí monitor LG 27''", "Náhrada za rozbitý monitor");

        // Doc 2: Submitted, pending účetní
        Long doc2 = createDoc(r2, DocumentType.CASH_RECEIPT, "Globus", "60193336",
                new BigDecimal("450.00"), new BigDecimal("545.00"),
                "Občerstvení pro klientskou schůzku",
                "Pohoštění během prezentace nového produktu");
        documentService.submitToAccounting(doc2, r2);

        // Doc 3: Submitted + posted
        Long doc3 = createDoc(r3, DocumentType.INVOICE, "ESET ČR", "27130864",
                new BigDecimal("2400.00"), new BigDecimal("2904.00"),
                "Roční licence ESET", "Antivirus pro celou firmu");
        documentService.submitToAccounting(doc3, r3);
        documentService.markAsPosted(doc3, admin, "FA-2026-0142");

        // Doc 4: Submitted + posted + paid (full cycle)
        Long doc4 = createDoc(r1, DocumentType.INVOICE, "CZC.cz", "25655701",
                new BigDecimal("12500.00"), new BigDecimal("15125.00"),
                "Síťové vybavení — switche", "Rozšíření síťové infrastruktury");
        documentService.submitToAccounting(doc4, r1);
        documentService.markAsPosted(doc4, admin, "FA-2026-0143");
        documentService.markAsPaid(doc4, admin);

        // Doc 5: Returned for completion
        Long doc5 = createDoc(r2, DocumentType.OTHER_LIABILITY, "Neznámý dodavatel", "00000000",
                new BigDecimal("3500.00"), new BigDecimal("4235.00"),
                "Faktura bez specifikace", "Doplnit popis nákladu");
        documentService.submitToAccounting(doc5, r2);
        documentService.returnForCompletion(doc5, admin, "Chybí číslo objednávky a popis zboží.");

        log.info("Demo data: created 5 accounting documents.");
        log.info("Demo data: seeding complete.");
    }

    private Long createDraft(User requester, String title, RequestType type,
                              BigDecimal amount, String supplier, String department,
                              String description, String justification) {
        CreateApprovalRequestDto dto = new CreateApprovalRequestDto();
        dto.setTitle(title);
        dto.setType(type);
        dto.setAmount(amount);
        dto.setCurrency("CZK");
        dto.setSupplier(supplier);
        dto.setDepartment(department);
        dto.setDescription(description);
        dto.setJustification(justification);
        dto.setRequestedDate(LocalDate.now().plusDays(7));
        ApprovalRequest req = requestService.createDraft(dto, requester);
        return req.getId();
    }

    private Long createDoc(User submitter, DocumentType type, String supplierName,
                            String supplierIco, BigDecimal netto, BigDecimal brutto,
                            String description, String justification) {
        CreateAccountingDocumentDto dto = new CreateAccountingDocumentDto();
        dto.setType(type);
        dto.setSupplierName(supplierName);
        dto.setSupplierIco(supplierIco);
        dto.setAmountWithoutVat(netto);
        dto.setAmountWithVat(brutto);
        dto.setDescription(description);
        dto.setJustification(justification);
        dto.setDepartment("IT");
        dto.setTaxDate(LocalDate.now());
        AccountingDocument doc = documentService.create(dto, submitter);
        return doc.getId();
    }

    private void approveIfPossible(Long requestId, String comment) {
        try {
            ApprovalRequest req = requestRepository.findById(requestId).orElseThrow();
            // Find the active step's approver
            req.getSteps().stream()
                    .filter(s -> s.getStatus() == bachelor.code.enums.StepStatus.PENDING)
                    .min((a, b) -> Integer.compare(a.getStepOrder(), b.getStepOrder()))
                    .ifPresent(step -> workflowService.approve(requestId, step.getApprover(), comment));
        } catch (Exception e) {
            log.warn("Demo: approveIfPossible({}) failed: {}", requestId, e.getMessage());
        }
    }

    private void rejectIfPossible(Long requestId, String comment) {
        try {
            ApprovalRequest req = requestRepository.findById(requestId).orElseThrow();
            req.getSteps().stream()
                    .filter(s -> s.getStatus() == bachelor.code.enums.StepStatus.PENDING)
                    .min((a, b) -> Integer.compare(a.getStepOrder(), b.getStepOrder()))
                    .ifPresent(step -> workflowService.reject(requestId, step.getApprover(), comment));
        } catch (Exception e) {
            log.warn("Demo: rejectIfPossible({}) failed: {}", requestId, e.getMessage());
        }
    }

    private void returnIfPossible(Long requestId, String comment) {
        try {
            ApprovalRequest req = requestRepository.findById(requestId).orElseThrow();
            req.getSteps().stream()
                    .filter(s -> s.getStatus() == bachelor.code.enums.StepStatus.PENDING)
                    .min((a, b) -> Integer.compare(a.getStepOrder(), b.getStepOrder()))
                    .ifPresent(step -> workflowService.returnForRevision(requestId, step.getApprover(), comment));
        } catch (Exception e) {
            log.warn("Demo: returnIfPossible({}) failed: {}", requestId, e.getMessage());
        }
    }
}
