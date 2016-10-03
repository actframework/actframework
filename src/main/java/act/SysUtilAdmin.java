package act;

import act.cli.CliContext;
import act.cli.Command;
import act.cli.Optional;
import act.cli.Required;
import act.util.PropertySpec;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class SysUtilAdmin {

    private CliContext context;

    public SysUtilAdmin() {
        context = CliContext.current();
    }

    @Command(name = "act.version, act.ver", help = "Print actframework version")
    public String version() {
        return Version.fullVersion().replace("-S-", "-SNAPSHOT-");
    }

    @Command(name = "act.pwd", help = "Print name of the current working directory")
    public String pwd() {
        return pwd(context).getAbsolutePath();
    }

    @Command(name = "act.ls, act.dir", help = "List files in the current working directory")
    @PropertySpec("path as name,size,timestamp")
    public List<FileInfo> ls(
            @Optional("specify the path to be listed") String path,
            @Optional(lead = "-a", help = "display hidden files") boolean showHidden,
            String path2,
            CliContext ctx
    ) {
        if (S.blank(path) && S.notBlank(path2)) {
            path = path2;
        }
        if (S.blank(path)) {
            ctx.println(pwd());
            return dir(curDir(), showHidden, context);
        } else {
            ctx.println(path);
            File file = getFile(path);
            if (!file.exists()) {
                ctx.println("%s is not a file or directory", path);
                return null;
            } else {
                if (file.isDirectory()) {
                    return dir(file, showHidden, context);
                } else {
                    return C.list(new FileInfo(file.getParentFile(), file));
                }
            }
        }
    }

    @Command(name = "act.cd", help = "Change working directory")
    public void cd(@Optional("specify the path to which the working directory to be changed") String path) {
        if (S.blank(path)) {
            context.println(pwd());
            return;
        }
        File file = getFile(path);
        if (path.contains("..")) {
            try {
                file = new File(file.getCanonicalPath());
            } catch (IOException e) {
                throw E.ioException(e);
            }
        }
        if (!file.isDirectory()) {
            context.println("path is not a directory");
            return;
        }
        context.chDir(file);
        context.println("current working directory changed to %s", file.getAbsolutePath());
    }

    @Command(name = "act.cat", help = "print file content")
    public void cat(
            @Required("specify the file to be printed out") File file,
            @Optional(help = "specify the maximum lines to be printed out", defVal = "20") int limit,
            @Optional(lead = "-n,--line-number", help = "print line number") boolean printLineNumber,
            CliContext context
    ) {
        if (isBinary(IO.is(file))) {
            context.println("binary file found");
            return;
        }
        context.println("");
        List<String> lines = IO.readLines(file, limit);
        int len = lines.size();
        for (int i = 0; i < len; ++i) {
            String line = lines.get(i);
            if (printLineNumber) {
                context.print("%5s | ", i + 1);
            }
            context.println(line);
        }
    }


    private File getFile(String path) {
        return context.getFile(path);
    }

    private File curDir() {
        return pwd(context);
    }

    private static File pwd(CliContext context) {
        return context.curDir();
    }

    private static List<FileInfo> dir(File file, boolean showHidden, CliContext context) {
        C.List<FileInfo> list = C.newList();
        File[] files = file.listFiles();
        if (null == files) {
            context.println("Invalid dir: %s", file.getAbsolutePath());
            return C.list();
        }
        File parent = file.getAbsoluteFile();
        for (File f0 : files) {
            if (!showHidden && f0.isHidden()) {
                continue;
            }
            list.add(new FileInfo(parent, f0));
        }
        list = list.sorted();
        return list;
    }

    public static class FileInfo implements Comparable<FileInfo> {
        String context;
        String path;
        String size;
        boolean hidden;
        LocalDateTime timestamp;
        boolean isDir;
        private FileInfo(File parent, File file) {
            this.isDir = file.isDirectory();
            this.context = null == parent ? "/" : parent.getAbsolutePath();
            this.path = printPath(file);
            this.size = printSize(file);
            this.timestamp = LocalDateTime.fromDateFields(new Date(file.lastModified()));
            this.hidden = file.isHidden();
        }

        public String getContext() {
            return context;
        }

        public String getPath() {
            return path;
        }

        public String getSize() {
            return size;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public boolean isHidden() {
            return hidden;
        }

        @Override
        public int compareTo(FileInfo that) {
            if (this.isDir) {
                return that.isDir ? this.path.compareTo(that.path) : -1;
            }  else {
                return that.isDir ? 1 : this.path.compareTo(that.path);
            }
        }

        private String printPath(File file) {
            if (isDir) {
                return S.builder("[").append(file.getName()).append("]").toString();
            } else {
                return file.getName();
            }
        }

        private String printSize(File file) {
            String unit;
            long len = file.length();
            if (len < 1024L) {
                unit = "B";
            } else {
                len /= 1024L;
                if (len < 1024L) {
                    unit = "KB";
                } else {
                    len /= 1024L;
                    if (len < 1024L) {
                        unit = "MB";
                    } else {
                        len /= 1024L;
                        if (len < 1024L) {
                            unit = "GB";
                        } else {
                            len /= 1024L;
                            unit = "TB";
                        }
                    }
                }
            }
            return S.builder().append(len).append(unit).toString();
        }
    }

    /**
     *  Guess whether given file is binary. Just checks for anything under 0x09.
     */
    private static boolean isBinary(InputStream in) {
        try {
            int size = in.available();
            if (size > 1024) size = 1024;
            byte[] data = new byte[size];
            in.read(data);
            in.close();

            int ascii = 0;
            int other = 0;

            for (int i = 0; i < data.length; i++) {
                byte b = data[i];
                if (b < 0x09) return true;

                if (b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D) ascii++;
                else if (b >= 0x20 && b <= 0x7E) ascii++;
                else other++;
            }

            return other != 0 && 100 * other / (ascii + other) > 95;

        } catch (IOException e) {
            throw E.ioException(e);
        }
    }
}
