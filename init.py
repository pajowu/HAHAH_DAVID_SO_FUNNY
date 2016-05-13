import os
import sys
PLATFORMS = ["wear", "mobile"]
EXTENSIONS = (".properties", ".java", ".xml", ".gradle")
package = input("PackageID (for example com.example.wearapp): ")
name = input("App Name: ")
for platform in PLATFORMS:
    for dirpath, dirnames, filenames in os.walk(platform):
        for filename in filenames:
            if filename.endswith(EXTENSIONS):
                fn = os.path.join(dirpath, filename)
                with open(fn) as srcfile:
                    src = srcfile.read()
                src = src.replace("com.example.weartemplate", package)
                src = src.replace("WearTemplate", name)
                with open(fn, "w") as writefile:
                    writefile.write(src)
    parts = package.split(".")
    for i in range(len(parts)-1):
        dir_path = platform+"/src/main/java/"+"/".join(parts[:i+1])
        if not os.path.isdir(dir_path):
            os.mkdir(dir_path)
    new_path = platform+"/src/main/java/"+"/".join(parts)
    os.rename(platform+"/src/main/java/com/example/weartemplate", new_path)
    try:
        os.removedirs(platform+"/src/main/java/com/example")
    except:
        pass