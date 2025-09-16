package utils;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//Imports
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ExcelUtils {


    public static class Credential {
        public final String tenantId;
        public final String email;
        public final String password;


        public Credential(String tenantId, String email, String password) {
            this.tenantId = tenantId;
            this.email = email;
            this.password = password;
        }


        @Override
        public String toString() {
            return "Credential{" + "tenantId='" + tenantId + '\'' + ", email='" + email + '\'' + '}';
        }
    }
    /**
     * Reads credentials from an xlsx file. Expects headers in the first row:
     * TenantId | Email | Password (case-insensitive)
     */
    public static List<Credential> readCredentials(String filePath, String sheetName) throws Exception {
        List<Credential> list = new ArrayList<>();
        try (InputStream in = new FileInputStream(filePath);
             XSSFWorkbook wb = new XSSFWorkbook(in)) {


            Sheet sheet = (sheetName == null) ? wb.getSheetAt(0) : wb.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException("Sheet not found: " + sheetName);


            Iterator<Row> rows = sheet.iterator();
            if (!rows.hasNext()) return list; // empty sheet


// Read header -> find column indexes
            Row header = rows.next();
            int tenantCol = -1, emailCol = -1, passCol = -1;
            for (Cell c : header) {
                String val = c.getStringCellValue().trim().toLowerCase();
                if (val.equals("tenantid") || val.equals("tenant_id") || val.equals("tenant id")) tenantCol = c.getColumnIndex();
                else if (val.equals("email")) emailCol = c.getColumnIndex();
                else if (val.equals("password") || val.equals("pass")) passCol = c.getColumnIndex();
            }


            if (tenantCol == -1 || emailCol == -1 || passCol == -1)
                throw new IllegalArgumentException("Excel header must contain TenantId, Email, Password columns");


            while (rows.hasNext()) {
                Row r = rows.next();
                String tenant = getStringCell(r, tenantCol);
                String email = getStringCell(r, emailCol);
                String pass = getStringCell(r, passCol);
                if (email == null || email.isEmpty()) continue; // skip blank rows
                list.add(new Credential(tenant, email, pass));
            }
        }
        return list;
    }


    private static String getStringCell(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null) return "";
        switch (c.getCellType()) {
            case STRING:
                return c.getStringCellValue();
            case NUMERIC:
// numeric -> convert to long if it is integer-like else double
                double d = c.getNumericCellValue();
                long l = (long) d;
                if (Math.abs(d - l) < 1e-9) return String.valueOf(l);
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(c.getBooleanCellValue());
            case FORMULA:
                try { return c.getStringCellValue(); } catch (Exception ex) { return String.valueOf(c.getNumericCellValue()); }
            default:
                return "";
        }
    }
}

