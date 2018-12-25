package ru.runa.common.web.action;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import ru.runa.common.WebResources;
import ru.runa.common.web.Commons;
import ru.runa.common.web.HTMLUtils;
import ru.runa.common.web.Resources;
import ru.runa.common.web.form.ViewLogForm;
import ru.runa.wfe.commons.IoCommons;
import ru.runa.wfe.security.Permission;
import ru.runa.wfe.security.SecuredSingleton;
import ru.runa.wfe.service.delegate.Delegates;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dofs
 * @struts:action path="/viewLogs" name="viewLogForm" validate="false"
 * @struts.action-forward name="success" path="/displayLogs.do" redirect =
 * "false"
 */
public class ViewLogsAction extends ActionBase {
    public static final String ACTION_PATH = "/viewLogs";
    private static int limitLinesCount = WebResources.getViewLogsLimitLinesCount();
    private static int autoReloadTimeoutSec = WebResources.getViewLogsAutoReloadTimeout();

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) {
        try {
            Delegates.getAuthorizationService().checkAllowed(Commons.getUser(request.getSession()), Permission.ALL, SecuredSingleton.LOGS);

            String logDirPath = IoCommons.getLogDirPath();
            request.setAttribute("logDirPath", logDirPath);
            ViewLogForm form = (ViewLogForm) actionForm;
            if (form.getFileName() != null) {
                File file = new File(logDirPath, form.getFileName());

                if (form.getMode() == ViewLogForm.MODE_DOWNLOAD) {
                    String encodedFileName = HTMLUtils.encodeFileName(request, form.getFileName());
                    response.setHeader("Content-disposition", "attachment; filename=\"" + encodedFileName + "\"");
                    OutputStream os = response.getOutputStream();
                    Files.copy(file, os);
                    os.flush();
                    return null;
                }

                int allLinesCount = countLines(file);
                form.setAllLinesCount(allLinesCount);

                if (form.getLimitLinesCount() == 0) {
                    form.setLimitLinesCount(limitLinesCount);
                }

                request.setAttribute("pagingToolbar", createPagingToolbar(form));

                String logFileContent;
                if (form.getMode() == ViewLogForm.MODE_READBEGIN) {
                    logFileContent = wrapLines(searchLines(file, form, new ArrayList<>()), form, new ArrayList<>());
                } else {
                    logFileContent = wrapLines(searchLinesReverse(file, form, new ArrayList<>()), form, new ArrayList<>());
                }
                request.setAttribute("logFileContent", logFileContent);
            }

            form.setLimitLinesCount(form.getLimitLinesCount() == 0 ? limitLinesCount : form.getLimitLinesCount());
            request.setAttribute("autoReloadTimeoutSec", autoReloadTimeoutSec);
            return mapping.findForward(Resources.FORWARD_SUCCESS);
        } catch (Exception e) {
            addError(request, e);
            return mapping.findForward(Resources.FORWARD_FAILURE);
        }
    }
    
    private String wrapLines (String lines, ViewLogForm form, List<Integer> lineNumbers) {
        StringBuilder b = new StringBuilder(lines.length() + 200);
        b.append("<table class=\"log\"><tr><td class=\"lineNumbers\">");
        if (lineNumbers != null) {
            for (Integer num : lineNumbers) {
                b.append(num).append("<br>");
            }
        } else {
            for (int i = form.getStartLine(); i <= form.getEndLine(); i++) {
                b.append(i).append("<br>");
            }
        }
        b.append("</td><td class=\"content\">");
        b.append(lines);
        b.append("</td></tr></table>");
        return b.toString();
    }

    private int countLines(File file) throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            byte[] c = new byte[1024];
            int count = 0;
            int readChars;
            while ((readChars = is.read(c)) != -1) {
                for (int i = 0; i < readChars; i++) {
                    if (c[i] == '\n') {
                        count++;
                    }
                }
            }
            if (count > 0) {
                // count last line
                count++;
            }
            return count;
        } finally {
            Closeables.closeQuietly(is);
        }
    }

    private String createPagingToolbar(ViewLogForm form) {
        if (form.getAllLinesCount() > form.getLimitLinesCount()) {
            StringBuilder b = new StringBuilder();
            int n = form.getAllLinesCount() / form.getLimitLinesCount();
            if (form.getAllLinesCount() % form.getLimitLinesCount() != 0) {
                n++;
            }
            for (int i = 0; i < n; i++) {
                int startFrom = i * form.getLimitLinesCount() + 1;
                int endTo = startFrom + form.getLimitLinesCount() - 1;
                String text;
                if (i == n - 1) {
                    text = "[" + startFrom + "-*]";
                } else {
                    text = "[" + startFrom + "-" + endTo + "]";
                }
                String href = "/wfe" + ViewLogsAction.ACTION_PATH +
                        ".do?fileName=" + form.getFileName() +
                        "&mode=" + form .getMode() +
                        "&startLine=" + startFrom +
                        "&endLine=" + endTo +
                        "&searchContainsWord=" + String.valueOf(form.isSearchContainsWord()) +
                        "&searchCaseSensitive=" + String.valueOf(form.isSearchCaseSensitive()) +
                        "&searchErrors=" + String.valueOf(form.isSearchErrors()) +
                        "&searchWarns=" + String.valueOf(form.isSearchWarns()) +
                        "&limitLinesCount=" + form.getLimitLinesCount();
                b.append("<a href=\"").append(href).append("\">").append(text).append("</a>&nbsp;&nbsp;&nbsp;");
            }
            return b.toString();
        }
        return null;
    }

    private String searchLines(File file, ViewLogForm form, List<Integer> lineNumbers) throws IOException {
        try (LineNumberReader lReader = new LineNumberReader(new InputStreamReader(new FileInputStream(file)))) {
            StringBuilder b = new StringBuilder(1000);
            int i = 1;
            String line;

            if (form.getStartLine() != 1) {
                for (int j = 0; j < form.getStartLine(); j++) {
                    if (lReader.readLine() == null) {
                        break;
                    }
                }
            }

            while (((line = lReader.readLine()) != null)) {
                boolean result = true;

                if (form.isSearchContainsWord()) {
                    result = form.isSearchCaseSensitive() ? StringUtils.contains(line, form.getSearch()) : StringUtils.containsIgnoreCase(line, form.getSearch());
                }

                if (form.isSearchErrors()) {
                    result = result && StringUtils.contains(line, " ERROR ");
                }

                if (form.isSearchWarns()) {
                    result = result && StringUtils.contains(line, " WARN ");
                }

                if (result) {
                    line = StringEscapeUtils.escapeHtml(line);
                    line = line.replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
                    b.append(line).append("<br>");
                    lineNumbers.add(i);
                    if (lineNumbers.size() > limitLinesCount) {
                        break;
                    }
                }

                i++;
            }

            return b.toString();
        }
    }

    private String searchLinesReverse(File file, ViewLogForm form, List<Integer> lineNumbers) throws IOException {
        try (ReversedLinesFileReader rReader = new ReversedLinesFileReader(file)) {
            StringBuilder b = new StringBuilder(1000);
            int i = 1;
            String line;

            if (form.getStartLine() != 1) {
                for (int j = 0; j < form.getStartLine(); j++) {
                    if (rReader.readLine() == null) {
                        break;
                    }
                }
            }

            while (((line = rReader.readLine()) != null)) {
                boolean result = true;

                if (form.isSearchContainsWord()) {
                    result = form.isSearchCaseSensitive() ? StringUtils.contains(line, form.getSearch()) : StringUtils.containsIgnoreCase(line, form.getSearch());
                }

                if (form.isSearchErrors()) {
                    result = result && StringUtils.contains(line, " ERROR ");
                }

                if (form.isSearchWarns()) {
                    result = result && StringUtils.contains(line, " WARN ");
                }

                if (result) {
                    line = StringEscapeUtils.escapeHtml(line);
                    line = line.replaceAll("\\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
                    b.append(line).append("<br>");
                    lineNumbers.add(i);
                    if (lineNumbers.size() > limitLinesCount) {
                        break;
                    }
                }

                i++;
            }

            return b.toString();
        }
    }


}
