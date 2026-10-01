package com.example.HealthcareSystem_Backend.service;

import com.example.HealthcareSystem_Backend.dto.AuthDTOs.*;
import com.example.HealthcareSystem_Backend.dto.OperationalDTOs.*;
import com.example.HealthcareSystem_Backend.entity.*;
import com.example.HealthcareSystem_Backend.exception.BadRequestException;
import com.example.HealthcareSystem_Backend.exception.ResourceNotFoundException;
import com.example.HealthcareSystem_Backend.repository.*;
import com.example.HealthcareSystem_Backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HospitalServices {

        private final UserRepository userRepository;
        private final PatientRepository patientRepository;
        private final DoctorRepository doctorRepository;
        private final DepartmentRepository departmentRepository;
        private final AppointmentRepository appointmentRepository;
        private final MedicalRecordRepository medicalRecordRepository;
        private final LaboratoryTestRepository labRepository;
        private final MedicineRepository medicineRepository;
        private final PrescriptionRepository prescriptionRepository;
        private final InvoiceRepository invoiceRepository;
        private final PaymentRepository paymentRepository;
        private final AdmissionRepository admissionRepository;
        private final EmployeeRepository employeeRepository;
        private final AttendanceRepository attendanceRepository;
        private final LeaveRecordRepository leaveRecordRepository;
        private final AuditLogRepository auditLogRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authManager;

        // --- Audit Log Helper ---
        public void recordAudit(String username, String action, String module, String desc) {
                auditLogRepository.save(AuditLog.builder()
                                .username(username)
                                .action(action)
                                .module(module)
                                .description(desc)
                                .timestamp(LocalDateTime.now())
                                .build());
        }

        // --- Authentication & User Operations ---
        public LoginResponse authenticate(LoginRequest req) {
                authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
                User user = userRepository.findByUsername(req.getUsername())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                String jwt = jwtService.generateToken(user);
                recordAudit(user.getUsername(), "LOGIN", "AUTH", "User logged into system");
                return LoginResponse.builder()
                                .token(jwt)
                                .userId(user.getId())
                                .username(user.getUsername())
                                .role(user.getRole())
                                .build();
        }

        public User createUser(UserRequest req, String adminUser) {
                if (userRepository.existsByUsername(req.getUsername())) {
                        throw new BadRequestException("Username already exists");
                }
                User user = User.builder()
                                .username(req.getUsername())
                                .password(passwordEncoder.encode(req.getPassword()))
                                .fullName(req.getFullName())
                                .email(req.getEmail())
                                .role(req.getRole())
                                .active(true)
                                .build();
                User saved = userRepository.save(user);
                recordAudit(adminUser, "CREATE", "USER", "Created user " + saved.getUsername());
                return saved;
        }

        // --- Patient Operations ---
        public Patient registerPatient(PatientRequest req, String username) {
                String patientNumber = "PAT-" + System.currentTimeMillis() % 1000000;
                Patient patient = Patient.builder()
                                .patientNumber(patientNumber)
                                .firstName(req.getFirstName())
                                .lastName(req.getLastName())
                                .dateOfBirth(req.getDateOfBirth())
                                .gender(req.getGender())
                                .nic(req.getNic())
                                .phone(req.getPhone())
                                .email(req.getEmail())
                                .address(req.getAddress())
                                .emergencyContact(req.getEmergencyContact())
                                .bloodGroup(req.getBloodGroup())
                                .conditionSummary(req.getConditionSummary())
                                .registrationDate(LocalDate.now())
                                .build();
                Patient saved = patientRepository.save(patient);
                recordAudit(username, "CREATE", "PATIENT", "Registered patient: " + patientNumber);
                return saved;
        }

        @Transactional
        public void deletePatient(Long patientId, String adminUser) {
                Patient patient = patientRepository.findById(patientId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Patient not found with id: " + patientId));

                String patientRef = patient.getPatientNumber() != null ? patient.getPatientNumber()
                                : "ID #" + patientId;

                // Dissociate or delete dependent records prior to removing patient
                appointmentRepository.deleteByPatientId(patientId);
                medicalRecordRepository.deleteByPatientId(patientId);
                labRepository.deleteByPatientId(patientId);
                admissionRepository.deleteByPatientId(patientId);

                patientRepository.delete(patient);
                recordAudit(adminUser, "DELETE", "PATIENT", "Admin deleted patient: " + patientRef);
        }

        // --- Doctor Operations ---
        @Transactional
        public void deleteDoctor(Long doctorId, String adminUser) {
                Doctor doctor = doctorRepository.findById(doctorId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Doctor not found with id: " + doctorId));

                String doctorDisplayName = doctor.getFullName() != null
                                ? doctor.getFullName()
                                : (doctor.getFirstName() != null ? doctor.getFirstName() + " " + doctor.getLastName()
                                                : "ID #" + doctorId);

                // Remove linked operational records first to satisfy foreign key constraints
                appointmentRepository.deleteByDoctorId(doctorId);
                medicalRecordRepository.deleteByDoctorId(doctorId);

                doctorRepository.delete(doctor);
                recordAudit(adminUser, "DELETE", "DOCTOR", "Admin removed doctor: " + doctorDisplayName);
        }

        // --- Appointment Operations ---
        public Appointment bookAppointment(AppointmentRequest req, String username) {
                Patient p = patientRepository.findById(req.getPatientId())
                                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
                Doctor d = doctorRepository.findById(req.getDoctorId())
                                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

                // Conflict check: within 15 minutes of slot
                LocalDateTime start = req.getAppointmentTime().minusMinutes(14);
                LocalDateTime end = req.getAppointmentTime().plusMinutes(14);
                List<Appointment> conflicts = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                                d.getId(), start, end);
                if (!conflicts.isEmpty()) {
                        throw new BadRequestException("Doctor already has an appointment scheduled near this time");
                }

                Appointment app = Appointment.builder()
                                .patient(p)
                                .doctor(d)
                                .appointmentTime(req.getAppointmentTime())
                                .reason(req.getReason())
                                .status(Appointment.Status.SCHEDULED)
                                .build();
                Appointment saved = appointmentRepository.save(app);
                recordAudit(username, "CREATE", "APPOINTMENT", "Booked appointment for patient ID: " + p.getId());
                return saved;
        }

        // --- Clinical & Medical Records ---
        public MedicalRecord addMedicalRecord(MedicalRecordRequest req, String username) {
                Patient p = patientRepository.findById(req.getPatientId())
                                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
                Doctor d = doctorRepository.findById(req.getDoctorId())
                                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

                MedicalRecord record = MedicalRecord.builder()
                                .patient(p)
                                .doctor(d)
                                .symptoms(req.getSymptoms())
                                .diagnosis(req.getDiagnosis())
                                .treatment(req.getTreatment())
                                .visitDate(LocalDateTime.now())
                                .build();
                MedicalRecord saved = medicalRecordRepository.save(record);
                recordAudit(username, "CREATE", "MEDICAL_RECORD", "Added record for patient: " + p.getPatientNumber());
                return saved;
        }

        // --- Pharmacy Management ---
        @Transactional
        public Prescription createPrescription(PrescriptionRequest req, String username) {
                Patient p = patientRepository.findById(req.getPatientId())
                                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
                Doctor d = doctorRepository.findById(req.getDoctorId())
                                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

                Prescription pres = Prescription.builder()
                                .patient(p)
                                .doctor(d)
                                .issueDate(LocalDateTime.now())
                                .status("PENDING")
                                .items(new ArrayList<>())
                                .build();

                Prescription savedPres = prescriptionRepository.save(pres);

                for (PrescriptionItemRequest itemReq : req.getItems()) {
                        Medicine med = medicineRepository.findById(itemReq.getMedicineId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
                        PrescriptionItem item = PrescriptionItem.builder()
                                        .prescription(savedPres)
                                        .medicine(med)
                                        .quantity(itemReq.getQuantity())
                                        .dosage(itemReq.getDosage())
                                        .build();
                        savedPres.getItems().add(item);
                }
                return prescriptionRepository.save(savedPres);
        }

        @Transactional
        public void dispensePrescription(Long prescriptionId, String username) {
                Prescription pres = prescriptionRepository.findById(prescriptionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));

                if ("DISPENSED".equalsIgnoreCase(pres.getStatus())) {
                        throw new BadRequestException("Prescription is already dispensed");
                }

                for (PrescriptionItem item : pres.getItems()) {
                        Medicine med = item.getMedicine();
                        if (med.getExpiryDate().isBefore(LocalDate.now())) {
                                throw new BadRequestException("Cannot dispense expired medicine: " + med.getName());
                        }
                        if (med.getQuantity() < item.getQuantity()) {
                                throw new BadRequestException("Insufficient stock for medicine: " + med.getName());
                        }
                        med.setQuantity(med.getQuantity() - item.getQuantity());
                        medicineRepository.save(med);
                }

                pres.setStatus("DISPENSED");
                prescriptionRepository.save(pres);
                recordAudit(username, "DISPENSE", "PHARMACY", "Dispensed prescription #" + prescriptionId);
        }

        // --- Billing & Payments ---
        @Transactional
        public Payment processPayment(PaymentRequest req, String username) {
                Invoice invoice = invoiceRepository.findById(req.getInvoiceId())
                                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

                double newPaid = invoice.getPaidAmount() + req.getAmount();
                if (newPaid > invoice.getTotalAmount()) {
                        throw new BadRequestException("Paid amount cannot exceed total bill");
                }
                invoice.setPaidAmount(newPaid);
                if (newPaid >= invoice.getTotalAmount()) {
                        invoice.setStatus(Invoice.Status.PAID);
                } else {
                        invoice.setStatus(Invoice.Status.PARTIALLY_PAID);
                }
                invoiceRepository.save(invoice);

                Payment payment = Payment.builder()
                                .invoice(invoice)
                                .amount(req.getAmount())
                                .paymentMethod(req.getPaymentMethod())
                                .paymentDate(LocalDateTime.now())
                                .receiptNumber("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .build();
                Payment saved = paymentRepository.save(payment);
                recordAudit(username, "PAYMENT", "BILLING",
                                "Recorded payment of " + req.getAmount() + " for invoice #" + invoice.getId());
                return saved;
        }
}