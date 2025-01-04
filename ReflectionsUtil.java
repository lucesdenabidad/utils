package datta.dev.plugin.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReflectionsUtil {

    public static List<Class<?>> getClassesFromPackage(Class<?> parentClass, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        packageName = packageName.replace("/", ".");

        try {
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(ReflectionsUtil.class);
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            if (jarFile.isFile()) {
                try (JarFile jar = new JarFile(jarFile)) {
                    String packagePath = packageName.replace('.', '/');
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.endsWith(".class") && name.startsWith(packagePath)) {
                            String className = name.replace('/', '.')
                                    .substring(0, name.length() - 6);

                            try {
                                Class<?> clazz = Class.forName(className);
                                if (parentClass.isAssignableFrom(clazz) && !clazz.equals(parentClass)) {
                                    classes.add(clazz);
                                }
                            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                            }
                        }
                    }
                }
            } else {
                ClassLoader classLoader = plugin.getClass().getClassLoader();
                String path = packageName.replace('.', '/');
                Enumeration<URL> resources = classLoader.getResources(path);

                while (resources.hasMoreElements()) {
                    File directory = new File(resources.nextElement().getFile());
                    if (directory.exists()) {
                        String[] files = directory.list();
                        if (files != null) {
                            for (String file : files) {
                                if (file.endsWith(".class")) {
                                    String className = packageName + '.' + file.substring(0, file.length() - 6);
                                    try {
                                        Class<?> clazz = Class.forName(className);
                                        if (parentClass.isAssignableFrom(clazz) && !clazz.equals(parentClass)) {
                                            classes.add(clazz);
                                        }
                                    } catch (ClassNotFoundException | NoClassDefFoundError ignored) {

                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return classes;
    }
}