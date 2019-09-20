package fr.cnes.regards.modules.acquisition.service.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Simon MILHAU
 *
 */
@Plugin(id = "SWHFileValidation", version = "1.0.0-SNAPSHOT", description = "SWH file validation plugin",
        author = "REGARDS Team", contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class SWHFileValidation implements IValidationPlugin {

    @Autowired
    private INotificationClient notificationClient;

    /**
     * {@link ITemplateService} instance
     */
    @Autowired
    private ITemplateService templateService;

    @PluginParameter(
            name = "thumbnailMandatory",
            description = "Is thumbnail file mandatory to generate the SIP ?",
            label = "Thumbnail mandatory",
            optional = true)
    private boolean thumbnailMandatory = true;

    @Override
    public boolean validate(Path filePath) {
        if ((filePath != null) && Files.isRegularFile(filePath) && Files.isReadable(filePath) && filePath.toString().endsWith(".zip"))
        {
            File imageFile = new File(filePath.toString().substring(0, filePath.toString().lastIndexOf('.') + 1) + "jpg");
            if (!imageFile.exists() && thumbnailMandatory) {
                    notificationClient
                            .notify("The preview jpeg file : " + imageFile.getPath() + " was not found aside the zip : " + filePath.toString(),
                                    "There was a problem when collecting the preview image for the SIP generation",
                                    NotificationLevel.WARNING,
                                    MimeTypeUtils.TEXT_HTML,
                                    DefaultRole.PROJECT_ADMIN);
            }
            return true;
        }
        return false;
    }
}
