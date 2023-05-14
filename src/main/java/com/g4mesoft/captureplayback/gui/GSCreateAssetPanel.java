package com.g4mesoft.captureplayback.gui;

import static com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel.translatable;

import java.util.UUID;

import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.common.asset.GSAssetInfo;
import com.g4mesoft.captureplayback.common.asset.GSDecodedAssetFile;
import com.g4mesoft.captureplayback.common.asset.GSEAssetNamespace;
import com.g4mesoft.captureplayback.common.asset.GSEAssetType;
import com.g4mesoft.captureplayback.common.asset.GSIAssetHistory;
import com.g4mesoft.captureplayback.module.client.GSClientAssetManager;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSEPopupPlacement;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSPanel;
import com.g4mesoft.ui.panel.GSPanelContext;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.GSPopup;
import com.g4mesoft.ui.panel.button.GSButton;
import com.g4mesoft.ui.panel.dropdown.GSDropdownList;
import com.g4mesoft.ui.panel.event.GSKeyButtonStroke;
import com.g4mesoft.ui.panel.event.GSKeyEvent;
import com.g4mesoft.ui.panel.field.GSTextField;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.renderer.GSIRenderer2D;

import net.minecraft.text.Text;

public class GSCreateAssetPanel extends GSParentPanel {

	private static final int BACKGROUND_COLOR = 0xFF252526;
	
	private static final Text CREATE_TITLE    = translatable("popup.createTitle");
	private static final Text DUPLICATE_TITLE = translatable("popup.duplicateTitle");
	private static final Text IMPORT_TITLE    = translatable("popup.importTitle");
	
	private static final Text NAME_TEXT       = translatable("popup.name");
	private static final Text TYPE_TEXT       = translatable("popup.type");
	private static final Text NAMESPACE_TEXT  = translatable("popup.namespace");
	private static final Text HANDLE_TEXT     = translatable("popup.handle");
	private static final Text CREATE_TEXT     = translatable("create");
	private static final Text CANCEL_TEXT     = translatable("cancel");
	
	private static final int OUTER_MARGIN  = 10;
	private static final int TITLE_MARGIN  = 10;
	private static final int BUTTON_MARGIN = 5;
	private static final int FIELD_MARGIN  = 5;
	
	private static final int FIELD_WIDTH = 135;
	
	private static final String NAME_COPY_KEY = "gui.tab.capture-playback.nameCopy";
	
	private final GSClientAssetManager assetManager;
	private final GSIAssetHistory history;
	// Initialized in their respective constructor
	private GSAssetInfo originalInfo;
	private GSDecodedAssetFile assetFile;
	
	private final GSTextLabel titleLabel;
	private final GSTextField nameField;
	private final GSDropdownList<Text> typeField;
	private final GSDropdownList<Text> namespaceField;
	private final GSTextField handleField;
	private final GSButton createButton;
	private final GSButton cancelButton;

	private GSCreateAssetPanel(GSClientAssetManager assetManager) {
		this.assetManager = assetManager;
		this.history = assetManager.getAssetHistory();
		
		titleLabel = new GSTextLabel(CREATE_TEXT);
		nameField = new GSTextField();
		typeField = new GSDropdownList<>(GSAssetHistoryPanel.TYPE_TEXTS);
		typeField.setEmptySelectionAllowed(false);
		namespaceField = new GSDropdownList<>(GSAssetHistoryPanel.NAMESPACE_TEXTS);
		namespaceField.setEmptySelectionAllowed(false);
		handleField = new GSTextField();
		createButton = new GSButton(CREATE_TEXT);
		cancelButton = new GSButton(CANCEL_TEXT);

		handleField.setEditable(false);
		
		initLayout();
		initEventListeners();
	}
	
	private GSCreateAssetPanel(GSClientAssetManager assetManager, GSAssetInfo originalInfo) {
		this(assetManager);
		
		this.originalInfo = originalInfo;
		
		titleLabel.setText((originalInfo != null) ? DUPLICATE_TITLE : CREATE_TITLE);
		
		if (originalInfo != null) {
			nameField.setText(copyOfName(originalInfo.getAssetName()));
			typeField.setSelectedIndex(originalInfo.getType().getIndex());
			namespaceField.setSelectedIndex(originalInfo.getHandle().getNamespace().getIndex());
			// Type should be the same as original asset
			typeField.setEnabled(false);
		} else {
			updateHandle();
		}
	}
	
	private GSCreateAssetPanel(GSClientAssetManager assetManager, GSDecodedAssetFile assetFile) {
		this(assetManager);
		
		if (assetFile == null)
			throw new IllegalArgumentException("assetFile is null");
		this.assetFile = assetFile;
		
		titleLabel.setText(IMPORT_TITLE);
		nameField.setText(assetFile.getAsset().getName());
		typeField.setSelectedIndex(assetFile.getHeader().getType().getIndex());
		// Type should be the same as imported asset
		typeField.setEnabled(false);
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		//         Create Asset
		//
		//            -------------------
		// Name:      |                 |
		//            -------------------
		//            -------------------
		// Type:      | Composition | V |
		//            -------------------
		//            -------------------
		// Namespace: | World       | V |
		//            -------------------
		//            -------------------
		// Handle:    |                 |
		//            -------------------
		//
		// ----------          ----------
		// | Create |          | Cancel |
		// ----------          ----------
		
		titleLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.GRID_WIDTH, 2)
			.set(GSGridLayoutManager.TOP_MARGIN, TITLE_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, TITLE_MARGIN);
		add(titleLabel);
		GSPanel contentPanel = new GSParentPanel();
		contentPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 1)
			.set(GSGridLayoutManager.GRID_WIDTH, 2)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH)
			.set(GSGridLayoutManager.LEFT_MARGIN, OUTER_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, OUTER_MARGIN);
		add(contentPanel);
		createButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, OUTER_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, OUTER_MARGIN);
		add(createButton);
		cancelButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.EAST)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, OUTER_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, OUTER_MARGIN);
		add(cancelButton);

		initContentLayout(contentPanel);
		
		setProperty(PREFERRED_HEIGHT, 200);
	}
	
	private void initContentLayout(GSPanel contentPanel) {
		contentPanel.setLayoutManager(new GSGridLayoutManager());
		
		int gridY = 0;
		GSTextLabel nameLabel = new GSTextLabel(NAME_TEXT);
		nameLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN);
		contentPanel.add(nameLabel);
		nameField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.LEFT_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.PREFERRED_WIDTH, FIELD_WIDTH);
		contentPanel.add(nameField);

		GSTextLabel typeLabel = new GSTextLabel(TYPE_TEXT);
		typeLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN);
		contentPanel.add(typeLabel);
		typeField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.LEFT_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN);
		contentPanel.add(typeField);

		GSTextLabel namespaceLabel = new GSTextLabel(NAMESPACE_TEXT);
		namespaceLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN);
		contentPanel.add(namespaceLabel);
		namespaceField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.LEFT_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, FIELD_MARGIN);
		contentPanel.add(namespaceField);

		GSTextLabel handleLabel = new GSTextLabel(HANDLE_TEXT);
		handleLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, gridY)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.RIGHT_MARGIN, FIELD_MARGIN);
		contentPanel.add(handleLabel);
		handleField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, gridY++)
			.set(GSGridLayoutManager.FILL, GSEFill.HORIZONTAL)
			.set(GSGridLayoutManager.LEFT_MARGIN, FIELD_MARGIN)
			.set(GSGridLayoutManager.PREFERRED_WIDTH, FIELD_WIDTH);
		contentPanel.add(handleField);
	}

	private void initEventListeners() {
		nameField.addChangeListener(this::updateHandle);
		namespaceField.addChangeListener(this::updateHandle);
		createButton.addActionListener(this::createAndHide);
		cancelButton.addActionListener(this::hide);
		putButtonStroke(new GSKeyButtonStroke(GSKeyEvent.KEY_ENTER), this::createAndHide);
		putButtonStroke(new GSKeyButtonStroke(GSKeyEvent.KEY_ESCAPE), this::hide);
	}
	
	private void updateHandle() {
		String name = getName();
		GSEAssetNamespace namespace = getNamespace();
		GSAssetHandle handle = GSAssetHandle.fromNameUnique(namespace, name, history::containsHandle);
		handleField.setText(handle.toString());
	}
	
	private String copyOfName(String name) {
		return GSPanelContext.i18nTranslateFormatted(NAME_COPY_KEY, name);
	}

	public String getName() {
		return nameField.getText();
	}
	
	public GSEAssetType getType() {
		int index = typeField.getSelectedIndex();
		return GSEAssetType.fromIndex(index);
	}

	public GSEAssetNamespace getNamespace() {
		int index = namespaceField.getSelectedIndex();
		return GSEAssetNamespace.fromIndex(index);
	}
	
	public GSAssetHandle getHandle() {
		GSAssetHandle handle = null;
		try {
			handle = GSAssetHandle.fromString(handleField.getText());
		} catch (IllegalArgumentException ignore) {
			// Parsing exception
		}
		return handle;
	}
	
	private void createAndHide() {
		if (assetFile != null) {
			assetManager.importAsset(getName(), getHandle(), assetFile);
		} else {
			UUID originalAssetUUID = (originalInfo != null) ?
					originalInfo.getAssetUUID() : null;
			assetManager.createAsset(getName(), getType(),
					getHandle(), originalAssetUUID);
		}
		hide();
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, BACKGROUND_COLOR);
		
		super.render(renderer);
	}
	
	/**
	 * Shows the <i>create asset</i> popup used to create an empty asset.
	 * 
	 * @param source - the popup source
	 * @param assetManager - the asset manager
	 * 
	 * @return the popup on which the create asset panel was shown.
	 */
	public static GSCreateAssetPanel show(GSPanel source, GSClientAssetManager assetManager) {
		return show(source, new GSCreateAssetPanel(assetManager, (GSAssetInfo)null));
	}
	
	/**
	 * Shows the <i>create asset</i> popup used to either create an empty asset
	 * if the given {@code originalInfo} is null, or a copy based on the asset
	 * with the given asset info.
	 * 
	 * @param source - the popup source
	 * @param assetManager - the asset manager
	 * @param originalInfo - the info of the asset which the new asset should
	 *                       be based on, or null if creating an empty asset.
	 * 
	 * @return the popup on which the create asset panel was shown.
	 */
	public static GSCreateAssetPanel show(GSPanel source, GSClientAssetManager assetManager, GSAssetInfo originalInfo) {
		return show(source, new GSCreateAssetPanel(assetManager, originalInfo));
	}

	/**
	 * Shows the <i>import asset</i> popup used to import the given decoded asset.
	 * 
	 * @param source - the popup source
	 * @param assetManager - the asset manager
	 * @param assetFile - the decoded asset to be imported
	 * 
	 * @return the popup on which the create asset panel was shown.
	 */
	public static GSCreateAssetPanel show(GSPanel source, GSClientAssetManager assetManager, GSDecodedAssetFile assetFile) {
		return show(source, new GSCreateAssetPanel(assetManager, assetFile));
	}
	
	private static GSCreateAssetPanel show(GSPanel source, GSCreateAssetPanel panel) {
		GSPopup popup = new GSPopup(panel, true);
		popup.setHiddenOnFocusLost(false);
		popup.setSourceFocusedOnHide(source != null);
		popup.show(source, 0, 0, GSEPopupPlacement.CENTER);
		return panel;
	}
	
	public void hide() {
		GSPanel parent = getParent();
		if (parent instanceof GSPopup)
			((GSPopup)parent).hide();
	}
}
