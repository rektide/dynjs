package org.dynjs.runtime.modules;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.dynjs.runtime.DynJS;
import org.dynjs.runtime.ExecutionContext;
import org.dynjs.runtime.builtins.Require;

public class CommonsVfsModuleProvider extends ModuleProvider {

    protected FileSystemManager fsManager;
    protected List<String> rootPaths;
    protected List<String> startPaths;

    protected boolean SANE = true;

    public List<String> DEFAULT_START_PATHS = Arrays.asList("res:", "file://");

    public CommonsVfsModuleProvider() throws FileSystemException {
        //DefaultFileSystemManager sfsm = new StandardFileSystemManager();
        //sfsm.init();
        FileSystemManager vfsm = VFS.getManager();
        this.fsManager = vfsm;
        this.startPaths = new ArrayList<String>(DEFAULT_START_PATHS);
    }

    public CommonsVfsModuleProvider(FileSystemManager fsManager, List<String> startPaths) {
        this.fsManager = fsManager;
        this.startPaths = startPaths;
    }

    @Override
    public String generateModuleID(ExecutionContext context, String moduleName) {
        Object require = context.getGlobalObject().get("require");
        if (require instanceof Require) {
            List<String> loadPaths = ((Require)require).getLoadPaths();
            try{
                FileObject moduleFile = findFile(loadPaths, moduleName);
                if (moduleFile != null && moduleFile.exists()) {
                    return moduleFile.getURL().toString();
                }
            } catch (FileSystemException e) {
                System.err.println("Error while checking for a module " + moduleName + ": " + e.getMessage());
            }
            try{
                FileObject moduleFile = findFile(loadPaths, moduleName+"/index.js");
                if (moduleFile != null && moduleFile.exists()) {
                    return moduleFile.getURL().toString();
                }
            } catch (FileSystemException e) {
                System.err.println("Error while checking for a module " + moduleName + ": " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    boolean load(DynJS runtime, ExecutionContext context, String moduleID) {
        try {
            FileObject file = fetchFile(moduleID);
            String fileContent = readFile(file);
            runtime.evaluate("require.addLoadPath('" + file.getParent() + "')");
            runtime.newRunner().withFileName(file.getURL().toString()).withContext(context).withSource(fileContent).execute();
            runtime.evaluate("require.removeLoadPath('" + file.getParent() + "')");
            return true;
        } catch (FileSystemException e) {
            System.err.println("There was an error loading the module " + moduleID + ". Error message: " + e.getMessage());
        }
        return false;
    }

    /**
     * Finds the module file based on the known load paths.
     *
     * @param loadPaths the list of load paths to search
     * @param moduleName the name of the module to find
     * @return the File if found, else null
     */
    protected FileObject findFile(List<String> loadPaths, String moduleName) {
        String fileName = normalizeName(moduleName);
        FileObject file = null;
        for (String loadPath : loadPaths) {
            // require('foo');
            for (String startPath : this.startPaths) {
                try {
                    String newName = startPath+loadPath+fileName;
                    file = fetchFile(newName);
                    if(file != null){
                        break;
                    }
                } catch (Exception ex) {
                    //System.out.println("Error getting file "+moduleName+": "+ex.getMessage());
                }
            }
        }
        return file;
    }

    protected FileObject fetchFile(String fileName) throws FileSystemException{
        FileObject file = this.fsManager.resolveFile(fileName);
        if (file.exists()) return file;
        throw new FileNotFoundException(fileName);
    }

    protected String readFile(FileObject file) throws FileSystemException {
        InputStream is = file.getContent().getInputStream();
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    protected String fetchAndReadFile(String fileName) throws FileSystemException {
        return readFile(fetchFile(fileName));
    }
}
