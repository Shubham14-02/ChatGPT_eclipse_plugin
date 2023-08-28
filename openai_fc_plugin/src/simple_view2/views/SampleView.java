package simple_view2.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;

import javax.inject.Inject;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IWorkbenchPage;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class SampleView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "simple_view2.views.SampleView";

	@Inject
	IWorkbench workbench;

	private TableViewer viewer;

	private Text expressionText;
	private Label resultLabel;
	private static final String OPENAI_API_KEY = "fd-DbVYwgexaqRltvCk2J1qT3BlbkFJNhukEiOPo2ryTVhsFjtN";

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		@Override
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		@Override
		public Image getImage(Object obj) {
			return workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}


	@Override
	public void createPartControl(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label expressionLabel = new Label(container, SWT.NONE);
		expressionLabel.setText("Request:");

		expressionText = new Text(container, SWT.BORDER);
		expressionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		
		// Create and initialize the TableViewer
		viewer = new TableViewer(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new ViewLabelProvider());
		Button openFileButton = new Button(container, SWT.PUSH);
		openFileButton.setText("Open file");

		resultLabel = new Label(container, SWT.NONE);
		resultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		openFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String userRequest = expressionText.getText();
				try {

					String location = getFile.process_it_please(userRequest);
					System.out.println(location);
					resultLabel.setText("File Path: " + location);
					container.layout(); // Update the layout to show the file path

					IFileStore file = EFS.getLocalFileSystem().getStore(new Path(location));
					System.out.println(file.getName());

					// Check if the file exists in the workspace

					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						IDE.openEditorOnFileStore(page, EFS.getLocalFileSystem().getStore(new Path(location)));
					} catch (PartInitException exz) {
						exz.printStackTrace();
					}

				} catch (IllegalArgumentException ex) {
					resultLabel.setText(ex.toString());
				}

			}
		});

		Button clearButton = new Button(container, SWT.PUSH);
		clearButton.setText("Clear");
		clearButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				expressionText.setText("");
				resultLabel.setText("");
				container.layout(); // Update the layout after clearing the request label
			}
		});
	}

	public class getFile {

		public static String openAIChat(String userMessage) {
			try {

				String model = "gpt-3.5-turbo-0613";
				String apiUrl = "https://api.openai.com/v1/chat/completions";
				URL url = new URL(apiUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");

				// Set the OpenAI API key in the request header using Basic Authentication
				String auth = ":" + OPENAI_API_KEY;
				byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
				String authHeaderValue = "Basic " + new String(encodedAuth);
				conn.setRequestProperty("Authorization", authHeaderValue);
				conn.setRequestProperty("Content-Type", "application/json");

				// Prepare the payload as JSON
				String payload = "{\n" + "  \"model\": \"" + model + "\",\n" + "  \"messages\": [\n"
						+ "    {\"role\": \"user\", \"content\": \"" + userMessage + "\"}\n" + "  ],\n"
						+ "  \"functions\": [\n" + "    {\n" + "      \"name\": \"get_file_path\",\n"
						+ "      \"description\": \"Get the path of the file to be retrieved\",\n"
						+ "      \"parameters\": {\n" + "        \"type\": \"object\",\n"
						+ "        \"properties\": {\n" + "          \"location\": {\n"
						+ "            \"type\": \"string\",\n"
						+ "            \"description\": \"The path of the file, e.g. 'Get movie.mp4 from the folder 'Good Will Hunting' in the movies folder in home folder' would result into filepath being home/movies/Good Will Hunting/movie.mp4, file path must start with 'home' folder and start the filepath with a frontslash, file path must end with the name of the file given\"\n"
						+ "          },\n" + "          \"type\": {\n" + "            \"type\": \"string\",\n"
						+ "            \"enum\": [\"get_file_path\"]\n" + "          }\n" + "        },\n"
						+ "        \"required\": [\"location\"]\n" + "      }\n" + "    }\n" + "  ]\n" + "}";

				// Send the POST request and read the response
				conn.setDoOutput(true);
				try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
					dos.write(payload.getBytes(StandardCharsets.UTF_8));
				}
				int responseCode = conn.getResponseCode();

				if (responseCode == 200) {
					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String inputLine;
					StringBuilder response = new StringBuilder();

					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
					return response.toString();
				} else {
					return "Error - Response Code: " + responseCode;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return "An error occurred while sending the request.";
			}
		}

		public static String process_it_please(String request) {

			String jsonResponse = openAIChat(request);
			System.out.println(jsonResponse);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = null;

			try {
				rootNode = mapper.readTree(jsonResponse);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			// Get the JSON argument from the response
			JsonNode argumentsNode = rootNode.at("/choices/0/message/function_call/arguments");
			String jsonArgument = argumentsNode.asText();

			// Parse the JSON argument to get the location
			JsonNode locationNode = null;
			try {
				locationNode = mapper.readTree(jsonArgument).at("/location");
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			String location = locationNode.asText();
			JsonNode functionNameNode = rootNode.at("/choices/0/message/function_call/name");
			String functionName = functionNameNode.asText();
			
			// Print the location and function to be called
			System.out.println("Func Name: " + functionName);
			System.out.println("Location: " + location);

			return location;

		}

	}


	@Override
	public void setFocus() {
		if (viewer != null && viewer.getControl() != null) {
			viewer.getControl().setFocus();
		}
	}
}
