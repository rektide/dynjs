package org.dynjs.runtime.modules;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.dynjs.runtime.DynJS;
import org.dynjs.runtime.ExecutionContext;
import org.dynjs.runtime.builtins.Require;

public class CommonsVfsModuleProvider extends ModuleProvider {

    protected FileSystemManager fsManager;

    protected boolean SANE = true;

    public CommonsVfsModuleProvider() throws FileSystemException {
        this.fsManager= VFS.getManager();
    }

	public CommonsVfsModuleProvider(FileSystemManager fsManager) {
        this.fsManager = fsManager;
    }

    @Override
    public String generateModuleID(ExecutionContext context, String moduleName) {
        Object require = context.getGlobalObject().get("require");
        if (require instanceof Require) {
            FileObject moduleFile = findFile(((Require)require).getLoadPaths(), moduleName);
            try {
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
        FileObject file;
        try{
            file = this.fsManager.resolveFile(moduleID);
        } catch (FileSystemException ex) {
            return false;
        }
        try {
            if(file == null || !file.exists())
                return false;
            runtime.evaluate("require.addLoadPath('" + file.getParent() + "')");
            runtime.newRunner().withContext(context).withSource(readFile(file)).execute();
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
            try {
                file = this.fsManager.resolveFile(loadPath+"/"+fileName);
                // foo.js is in the require path
                if (file.exists()) break;
            } catch (FileSystemException ex) {
            }
        }
        return file;
    }

    protected String readFile(FileObject file) throws FileSystemException {
        InputStream is = file.getContent().getInputStream();
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
