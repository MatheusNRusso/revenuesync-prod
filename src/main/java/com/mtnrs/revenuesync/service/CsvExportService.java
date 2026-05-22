package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.Lead;
import com.mtnrs.revenuesync.domain.Payment;
import com.mtnrs.revenuesync.domain.Conversion;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Service responsible for generating CSV exports for payments and conversions.
 * All methods return byte arrays ready for HTTP response.
 */
@Service
public class CsvExportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DecimalFormat CSV_NUMBER_FORMAT =
            (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
    static {
        CSV_NUMBER_FORMAT.setMinimumFractionDigits(2);
        CSV_NUMBER_FORMAT.setMaximumFractionDigits(2);
        CSV_NUMBER_FORMAT.setGroupingUsed(false);
    }

    /**
     * Exports payments list to CSV format.
     * @param payments List of Payment entities
     * @return CSV content as byte array (UTF-8 encoded)
     */
    public byte[] exportPaymentsToCsv(List<Payment> payments) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write("ID,External ID,Merchant ID,Customer Name,Customer Email,Amount,Currency,Status,Created At\n");

            for (Payment payment : payments) {
                writer.write(String.format("%d,%s,%d,\"%s\",\"%s\",%s,%s,%s,%s\n",
                        payment.getId(),
                        escapeCsv(payment.getExternalId()),
                        payment.getMerchant().getId(),
                        escapeCsv(payment.getCustomerName()),
                        escapeCsv(payment.getCustomerEmail()),
                        CSV_NUMBER_FORMAT.format(payment.getAmount()),
                        payment.getCurrency(),
                        payment.getStatus(),
                        payment.getCreatedAt().format(DATE_FORMAT)
                ));
            }
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export payments to CSV", e);
        }

        return outputStream.toByteArray();
    }


    /**
     * Exports conversions list to CSV format.
     * @param conversions List of Conversion entities
     * @return CSV content as byte array (UTF-8 encoded)
     */
    public byte[] exportConversionsToCsv(List<Conversion> conversions) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write("ID,Payment ID,Platform,Value,Request Payload,Response Payload,Created At\n");

            for (Conversion conversion : conversions) {
                writer.write(String.format("%d,%d,%s,%s,\"%s\",\"%s\",%s\n",
                        conversion.getId(),
                        conversion.getPaymentId(),
                        conversion.getPlatform(),
                        CSV_NUMBER_FORMAT.format(conversion.getValue()),
                        escapeCsv(conversion.getRequestPayload()),
                        escapeCsv(conversion.getResponsePayload()),
                        conversion.getCreatedAt().format(DATE_FORMAT)
                ));
            }
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export conversions to CSV", e);
        }

        return outputStream.toByteArray();
    }

    /**
     * Escapes CSV special characters (quotes, commas, newlines).
     * @param value The string value to escape
     * @return Escaped string safe for CSV format
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    public byte[] exportLeadsToCsv(List<Lead> leads) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write("ID,Name,Email,Source,Created At\n");

            for (Lead lead : leads) {
                writer.write(String.format("%d,\"%s\",\"%s\",%s,%s\n",
                        lead.getId(),
                        escapeCsv(lead.getName()),
                        escapeCsv(lead.getEmail()),
                        lead.getSource(),
                        lead.getCreatedAt().format(DATE_FORMAT)
                ));
            }
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export leads to CSV", e);
        }

        return outputStream.toByteArray();
    }
}