package com.example.HealthcareSystem_Backend.service;

import com.example.HealthcareSystem_Backend.dto.ReportDTOs.*;
import com.example.HealthcareSystem_Backend.entity.*;
import com.example.HealthcareSystem_Backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;
    private final AppointmentRepository appointmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LaboratoryTestRepository labRepository;
    private final EmployeeRepository employeeRepository;
    private final StaffAttendanceRepository attendanceRepository;
    private final LeaveRecordRepository leaveRecordRepository;

    // 1. Patient Reports
    public PatientReportDTO getPatientReport() {
        List<Patient> patients = patientRepository.findAll();
        long activeAdmissions = admissionRepository.findAll().stream()
                .filter(a -> "ADMITTED".equalsIgnoreCase(a.getStatus()))
                .count();

        Map<String, Long> genders = patients.stream()
                .filter(p -> p.getGender() != null && !p.getGender().isBlank())
                .collect(Collectors.groupingBy(Patient::getGender, Collectors.counting()));

        Map<String, Long> bloodGroups = patients.stream()
                .filter(p -> p.getBloodGroup() != null && !p.getBloodGroup().isBlank())
                .collect(Collectors.groupingBy(Patient::getBloodGroup, Collectors.counting()));

        List<PatientSummary> recent = patients.stream()
                .filter(p -> p.getId() != null)
                .sorted(Comparator.comparing(Patient::getId).reversed())
                .limit(10)
                .map(p -> PatientSummary.builder()
                        .patientNumber(p.getPatientNumber())
                        .fullName(p.getFirstName() + " " + p.getLastName())
                        .gender(p.getGender())
                        .bloodGroup(p.getBloodGroup())
                        .registrationDate(p.getRegistrationDate())
                        .build())
                .collect(Collectors.toList());

        return PatientReportDTO.builder()
                .totalRegistered(patients.size())
                .totalAdmittedNow(activeAdmissions)
                .genderDistribution(genders)
                .bloodGroupDistribution(bloodGroups)
                .recentRegistrations(recent)
                .build();
    }

    // 2. Appointment Reports
    public AppointmentReportDTO getAppointmentReport() {
        List<Appointment> apps = appointmentRepository.findAll();

        long scheduled = apps.stream().filter(a -> a.getStatus() == Appointment.Status.SCHEDULED).count();
        long completed = apps.stream().filter(a -> a.getStatus() == Appointment.Status.COMPLETED).count();
        long cancelled = apps.stream().filter(a -> a.getStatus() == Appointment.Status.CANCELLED).count();

        Map<Doctor, Long> doctorCounts = apps.stream()
                .filter(a -> a != null && a.getDoctor() != null)
                .collect(Collectors.groupingBy(Appointment::getDoctor, Collectors.counting()));

        List<DoctorAppointmentLoad> docLoads = doctorCounts.entrySet().stream()
                .map(entry -> DoctorAppointmentLoad.builder()
                        .doctorName(entry.getKey().getFullName())
                        .specialization(entry.getKey().getSpecialization())
                        .appointmentCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return AppointmentReportDTO.builder()
                .totalAppointments(apps.size())
                .scheduledCount(scheduled)
                .completedCount(completed)
                .cancelledCount(cancelled)
                .doctorLoads(docLoads)
                .build();
    }

    // 3. Revenue Reports
    public RevenueReportDTO getRevenueReport() {
        List<Invoice> invoices = invoiceRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        double totalBilled = invoices.stream()
                .filter(i -> i != null && i.getTotalAmount() != null)
                .mapToDouble(Invoice::getTotalAmount)
                .sum();

        double totalCollected = payments.stream()
                .filter(p -> p != null && p.getAmount() != null)
                .mapToDouble(Payment::getAmount)
                .sum();

        double outstanding = Math.max(0, totalBilled - totalCollected);

        long paidCount = invoices.stream().filter(i -> i.getStatus() == Invoice.Status.PAID).count();
        long pendingCount = invoices.stream().filter(i -> i.getStatus() != Invoice.Status.PAID).count();

        Map<String, Double> paymentMethods = payments.stream()
                .filter(p -> p != null && p.getPaymentMethod() != null && p.getAmount() != null)
                .collect(
                        Collectors.groupingBy(Payment::getPaymentMethod, Collectors.summingDouble(Payment::getAmount)));

        return RevenueReportDTO.builder()
                .totalBilled(totalBilled)
                .totalCollected(totalCollected)
                .outstandingBalance(outstanding)
                .paidInvoicesCount(paidCount)
                .pendingInvoicesCount(pendingCount)
                .paymentMethodBreakdown(paymentMethods)
                .build();
    }

    // 4. Pharmacy Reports
    public PharmacyReportDTO getPharmacyReport() {
        List<Medicine> medicines = medicineRepository.findAll();
        LocalDate now = LocalDate.now();

        List<LowStockAlert> lowStock = medicines.stream()
                .filter(m -> m.getQuantity() != null && m.getQuantity() < 20)
                .map(m -> LowStockAlert.builder()
                        .medicineName(m.getName())
                        .currentStock(m.getQuantity())
                        .expiryDate(m.getExpiryDate())
                        .build())
                .collect(Collectors.toList());

        long expiredCount = medicines.stream()
                .filter(m -> m.getExpiryDate() != null && m.getExpiryDate().isBefore(now))
                .count();

        long dispensed = prescriptionRepository.findAll().stream()
                .filter(p -> "DISPENSED".equalsIgnoreCase(p.getStatus()))
                .count();

        return PharmacyReportDTO.builder()
                .totalMedicines(medicines.size())
                .lowStockCount(lowStock.size())
                .expiredCount(expiredCount)
                .totalDispensedPrescriptions(dispensed)
                .lowStockAlerts(lowStock)
                .build();
    }

    // 5. Laboratory Reports
    public LaboratoryReportDTO getLaboratoryReport() {
        List<LaboratoryTest> tests = labRepository.findAll();

        long completed = tests.stream()
                .filter(t -> t.getStatus() != null && "COMPLETED".equalsIgnoreCase(t.getStatus().name()))
                .count();

        long pending = tests.stream()
                .filter(t -> t.getStatus() == null || !"COMPLETED".equalsIgnoreCase(t.getStatus().name()))
                .count();

        Map<String, Long> topTests = tests.stream()
                .filter(t -> t != null && t.getTestName() != null && !t.getTestName().isBlank())
                .collect(Collectors.groupingBy(LaboratoryTest::getTestName, Collectors.counting()));

        return LaboratoryReportDTO.builder()
                .totalTestsConducted(tests.size())
                .pendingResultsCount(pending)
                .completedResultsCount(completed)
                .topTestsConducted(topTests)
                .build();
    }

    // 6. Staff Reports
    public StaffReportDTO getStaffReport() {
        List<Employee> emps = employeeRepository.findAll();
        LocalDate today = LocalDate.now();

        long presentToday = attendanceRepository.findByAttendanceDate(today).size();
        long onLeave = leaveRecordRepository.findAll().stream()
                .filter(l -> l.getStatus() == LeaveRecord.LeaveStatus.APPROVED &&
                        !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                .count();

        Map<String, Long> deptHeads = emps.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment() != null ? e.getDepartment().getName() : "General",
                        Collectors.counting()));

        return StaffReportDTO.builder()
                .totalEmployees(emps.size())
                .activeCount(emps.stream().filter(e -> Boolean.TRUE.equals(e.getActive())).count())
                .onLeaveCount(onLeave)
                .presentTodayCount(presentToday)
                .departmentHeadcounts(deptHeads)
                .build();
    }
}