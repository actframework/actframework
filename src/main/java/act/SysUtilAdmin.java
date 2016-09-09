package act;

import act.app.CliContext;
import act.cli.Command;
import act.cli.Optional;
import act.inject.Context;
import act.util.PropertySpec;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.IOException;
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
    @PropertySpec("context,path,size")
    public List<FileInfo> ls(
            @Optional("specify the path to be listed") String path,
            CliContext ctx
    ) {
        if (S.blank(path)) {
            return dir(curDir(), context);
        } else {
            File file = getFile(path);
            if (!file.exists()) {
                ctx.println("%s is not a file or directory", path);
                return null;
            } else {
                if (file.isDirectory()) {
                    return dir(file, context);
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

    private File getFile(String path) {
        return context.getFile(path);
    }

    private File curDir() {
        return pwd(context);
    }

    private static File pwd(CliContext context) {
        return context.curDir();
    }

    private static List<FileInfo> dir(File file, CliContext context) {
        C.List<FileInfo> list = C.newList();
        File[] files = file.listFiles();
        if (null == files) {
            context.println("Invalid dir: %s", file.getAbsolutePath());
            return C.list();
        }
        File parent = file.getAbsoluteFile();
        for (File f0 : files) {
            list.add(new FileInfo(parent, f0));
        }
        list = list.sorted();
        return list;
    }

    public static class FileInfo implements Comparable<FileInfo> {
        String context;
        String path;
        String size;
        boolean isDir;
        private FileInfo(File parent, File file) {
            this.isDir = file.isDirectory();
            this.context = null == parent ? "/" : parent.getAbsolutePath();
            this.path = printPath(file);
            this.size = printSize(file);
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
}
