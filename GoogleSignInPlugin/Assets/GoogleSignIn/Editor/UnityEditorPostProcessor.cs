using System.IO;

using UnityEditor;
using UnityEditor.Callbacks;
using UnityEditor.iOS.Xcode;

namespace Google {
    public static class UnityEditorPostProessorForSignin {
        const string PLIST_FILE_NAME= "GoogleService-Info.plist";
        [PostProcessBuild]
        public static void OnPostProcessBuild(BuildTarget buildTarget, string path)
        {
            // Go get pbxproj file
            string projPath = path + "/Unity-iPhone.xcodeproj/project.pbxproj";

            // PBXProject class represents a project build settings file,
            // here is how to read that in.
            PBXProject proj = new PBXProject ();
            proj.ReadFromFile (projPath);

            // This is the Xcode target in the generated project
            string target = proj.TargetGuidByName ("Unity-iPhone");

            string[] files = Directory.GetFiles("Assets", PLIST_FILE_NAME, SearchOption.AllDirectories);
            if (files.Length > 0) {
                // Copy plist from the project folder to the build folder
                FileUtil.CopyFileOrDirectory (files[0], Path.Combine(path, PLIST_FILE_NAME));
                proj.AddFileToBuild (target, proj.AddFile(PLIST_FILE_NAME, PLIST_FILE_NAME));

                // add URLType
                var plistPath = Path.Combine(path, "Info.plist");
                var plistCred = new PlistDocument();
                plistCred.ReadFromFile(files[0]);
                var plistInfo = new PlistDocument();
                plistInfo.ReadFromFile(plistPath);
                var urlentry = plistInfo.root.CreateArray("CFBundleURLTypes")
                    .AddDict();
                urlentry.SetString("CFBundleTypeRole", "Editor");
                urlentry.CreateArray("CFBundleURLSchemes").AddString(
                    plistCred.root["REVERSED_CLIENT_ID"].AsString()
                );
                plistInfo.WriteToFile(plistPath);

                // Write PBXProject object back to the file
                proj.WriteToFile (projPath);
            }
        }
    }
}
