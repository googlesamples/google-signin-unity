#if UNITY_EDITOR && (UNITY_IOS || UNITY_TVOS || UNITY_VISIONOS)
using System;
using System.IO;
using System.Linq;

using UnityEditor;
using UnityEditor.iOS.Xcode;

using UnityEditor.Build;
using UnityEditor.Build.Reporting;

public class GameBuilder : IPostprocessBuildWithReport
{
	public int callbackOrder => 999;

	public void OnPostprocessBuild(BuildReport report)
    {
        if(report.summary.platform != BuildTarget.iOS)
            return;

        string infoPlist = Path.Combine(report.summary.outputPath,"Info.plist");
        if(!File.Exists(infoPlist))
            return;

        string googleScheme = PlayerSettings.iOS.iOSUrlSchemes.FirstOrDefault((scheme) => scheme.StartsWith("com.googleusercontent.apps."));
        if(string.IsNullOrEmpty(googleScheme))
            return;

        PlistDocument plistDoc = new();
        plistDoc.ReadFromFile(infoPlist);
        plistDoc.root.SetString("GIDClientID",string.Join('.',googleScheme.Split('.').Reverse()));

        plistDoc.WriteToFile(infoPlist);
    }
}
#endif