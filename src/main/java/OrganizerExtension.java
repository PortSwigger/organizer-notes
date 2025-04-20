import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;
import storages.CategoryStorage;
import storages.NotesStorage;

public class OrganizerExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Organizer Notes");

        NotesStorage notesStorage = new NotesStorage(api.persistence());
        CategoryStorage categoryStorage = new CategoryStorage(api.persistence());

        UserInterface ui = api.userInterface();

        OrganizerNotesTab organizerNotesTab = new OrganizerNotesTab(api.logging(), notesStorage, categoryStorage);
        ui.registerSuiteTab("OrgNotes", organizerNotesTab.getPanel());

        api.logging().logToOutput("Organizer Notes loaded!");

        api.userInterface().registerContextMenuItemsProvider(new SendToOrganizerMenu(api, notesStorage,categoryStorage));
    }
}
