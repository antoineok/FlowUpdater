package fr.flowarg.flowupdater.versions;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.flowarg.flowupdater.FlowUpdater;

public interface IVanillaVersion
{
	IVanillaVersion NULL_VERSION = new IVanillaVersion()
	{
		@Override
		public String getName()
		{
			return "null";
		}
		
		@Override
		public JsonObject getMinecraftServer()
		{
			return new JsonObject();
		}
		
		@Override
		public JsonArray getMinecraftLibrariesJson()
		{
			return new JsonArray(0);
		}
		
		@Override
		public JsonObject getMinecraftClient()
		{
			return new JsonObject();
		}
		
		@Override
		public JsonObject getMinecraftAssetsIndex()
		{
			return new JsonObject();
		}
	};
	
    String getName();

    JsonArray getMinecraftLibrariesJson();
    JsonObject getMinecraftClient();
    JsonObject getMinecraftServer();

    JsonObject getMinecraftAssetsIndex();

    class Builder
    {
        private String name;
        /** MCP Version to install, can be null if you want a vanilla/Forge installation */
        private MCP mcp = null;

        public Builder(String name)
        {
            this.name = name;
        }

        public IVanillaVersion build(boolean isSnapshot, VersionType versionType)
        {
            return new IVanillaVersion()
            {
                private final JsonElement JSON = this.readData(this.getJsonVersion());

                @Override
                public String getName()
                {
                    return Builder.this.name;
                }

                private InputStream getJsonVersion()
                {
                    final AtomicReference<String> version = new AtomicReference<>(null);
                    final AtomicReference<InputStream> result = new AtomicReference<>(null);

                    try
                    {
                        final JsonObject launcherMeta = this.readData(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()).getAsJsonObject();

                        if (this.getName().equals("latest"))
                        {
                            if (isSnapshot)
                                version.set(launcherMeta.getAsJsonObject("latest").get("snapshot").getAsString());
                            else version.set(launcherMeta.getAsJsonObject("latest").get("release").getAsString());
                        }
                        else version.set(this.getName());
                        launcherMeta.getAsJsonArray("versions").forEach(jsonElement ->
                        {
                            if (jsonElement.getAsJsonObject().get("id").getAsString().equals(version.get()))
                            {
                                try
                                {
                                    result.set(new URL(jsonElement.getAsJsonObject().get("url").getAsString()).openStream());
                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    return result.get();
                }

                @Override
                public JsonArray getMinecraftLibrariesJson()
                {
                    return JSON.getAsJsonObject().getAsJsonArray("libraries");
                }

                @Override
                public JsonObject getMinecraftClient()
                {
                	if(versionType == VersionType.MCP)
                	{
                		final JsonObject result = new JsonObject();
                		final String sha1 = Builder.this.getMcp().getClientSha1();
                		final String url = Builder.this.getMcp().getClientDownloadURL();
                		final int size = Builder.this.getMcp().getClientSize();
                		if(this.checkString(sha1) && this.checkString(url) && size > 0)
                		{
                    		result.addProperty("sha1", Builder.this.getMcp().getClientSha1());
                    		result.addProperty("size", Builder.this.getMcp().getClientSize());
                    		result.addProperty("url", Builder.this.getMcp().getClientDownloadURL());
                    		return result;
                		}
                		else FlowUpdater.DEFAULT_LOGGER.warn("Skipped MCP Client");
                	}
                	return JSON.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("client");
                }

                @Override
                public JsonObject getMinecraftServer()
                {
                	if(versionType == VersionType.MCP)
                	{
                		final JsonObject result = new JsonObject();
                		final String sha1 = Builder.this.getMcp().getServerSha1();
                		final String url = Builder.this.getMcp().getServerDownloadURL();
                		final int size = Builder.this.getMcp().getServerSize();
                		if(this.checkString(url) && this.checkString(sha1) && size > 0)
                		{
                    		result.addProperty("sha1", Builder.this.getMcp().getServerSha1());
                    		result.addProperty("size", Builder.this.getMcp().getServerSize());
                    		result.addProperty("url", Builder.this.getMcp().getServerDownloadURL());
                    		return result;
                		}
                		else FlowUpdater.DEFAULT_LOGGER.warn("Skipped MCP Server");
                	}
                	return JSON.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("server");
                }
                
                private boolean checkString(String str)
                {
                	return str != null && !str.trim().equals("");
                }

                @Override
                public JsonObject getMinecraftAssetsIndex()
                {
                    return JSON.getAsJsonObject().getAsJsonObject("assetIndex");
                }

                private JsonElement readData(InputStream input)
                {
                    try(InputStream stream = new BufferedInputStream(input))
                    {
                        final Reader reader = new BufferedReader(new InputStreamReader(stream));
                        final StringBuilder sb = new StringBuilder();

                        int character;
                        while ((character = reader.read()) != -1) sb.append((char)character);

                        return JsonParser.parseString(sb.toString());
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    return JsonNull.INSTANCE;
                }
            };
        }
        
        /**
         * Necessary if you want install a MCP version.
         * @param mcp MCP version to install.
         */
        public Builder withMCPVersion(MCP mcp)
        {
    		this.mcp = mcp;
    		return this;
    	}
        
        public MCP getMcp()
        {
    		return this.mcp;
    	}
    }
}
