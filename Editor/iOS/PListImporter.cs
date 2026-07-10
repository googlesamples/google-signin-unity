#if UNITY_EDITOR
using System.IO;

using UnityEngine;

using UnityEditor.AssetImporters;

[ScriptedImporter(1, "plist")]
public class PListImporter : ScriptedImporter
{
	public override void OnImportAsset(AssetImportContext ctx)
	{
		if(ctx.mainObject is TextAsset)
			return;

		var subAsset = new TextAsset(File.ReadAllText(ctx.assetPath));
		ctx.AddObjectToAsset("text", subAsset);
		ctx.SetMainObject(subAsset);
	}
}
#endif