package prerna.sablecc2.reactor.frame.gaas.qa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.AssetUtility;

public class PreprocessQAReactor extends AbstractReactor {

	// preprocesses documents in a given folder
	// essentially turns it into a text file and drops it into the folder / text directory
	
	public PreprocessQAReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.FILE_PATH.getKey(), ReactorKeysEnum.SEPARATOR.getKey()};
		this.keyRequired = new int[] {1, 0};
	}

	
	
	@Override
	public NounMetadata execute() {
		
		organizeKeys();
		String folderName = keyValue.get(keysToGet[0]);
		String separator = "=x=x=x=";
		if(keyValue.containsKey(keysToGet[1]))
			separator = keyValue.get(keysToGet[1]);
				
		// check if directory exists
		// create a text directory
		// pick all the pdf files
		// convert them to text file
		String basePath = AssetUtility.getProjectAssetFolder(this.insight.getProjectId());
		folderName = basePath + "/" + folderName;
		String txtFolderName = folderName + "/processed";

		File inputFolder = new File(folderName);
		if(!inputFolder.exists())
			return NounMetadata.getErrorNounMessage("No such folder ");
		
		// create the text directory
		File txtFolder = new File(txtFolderName);
		if(!txtFolder.exists())
		{
			txtFolder.mkdir();
		}
		
		StringBuffer processedFiles = new StringBuffer(" Processed Files : ");
		
		// grab all the pdf
		String [] inputFiles = inputFolder.list();
		for(int inputFileIndex = 0;inputFileIndex < inputFiles.length;inputFileIndex++)
		{
			String inputFileName = inputFiles[inputFileIndex];
			if(inputFileIndex > 0)
				processedFiles.append(",  ");
			if(inputFileName.endsWith(".pdf")) // this is a pdf file // need a better way
			{
				inputFileName = folderName + "/" + inputFileName;
				String documentName = FileNameUtils.getBaseName(inputFileName);
				processedFiles.append(documentName);
				String txtOutputFileName = txtFolder + "/" + documentName + ".txt" ;
				convertPDFFile2Text(inputFileName, txtOutputFileName, documentName, separator);
			}
		}
		return new NounMetadata(processedFiles + "", PixelDataType.CONST_STRING);
	}
	
	public void convertPDFFile2Text(String pdfFile, String outputFile, String documentName, String separator)
	{	
		try {
			File f = new File(pdfFile);
			String parsedText;
			PDFParser parser = new PDFParser(new RandomAccessFile(f, "r"));
			parser.parse();
			
			COSDocument cosDoc = parser.getDocument();
			
			PDFTextStripper pdfStripper = new PDFTextStripper();
			PrintWriter pw = new PrintWriter(outputFile);
			
			// get total number of pages
			PDDocument pdDoc = new PDDocument(cosDoc);
			int totalPages = pdDoc.getNumberOfPages();
			for(int pageIndex = 0;pageIndex < totalPages;pageIndex++)
			{
				pdfStripper.setStartPage(pageIndex);
				pdfStripper.setEndPage(pageIndex);
				parsedText = pdfStripper.getText(pdDoc);
				parsedText =  documentName + "::::Page <" + pageIndex + ">::::" + parsedText;
				//System.err.println(parsedText);
				pw.print(parsedText);
				pw.print(separator);
				pw.flush();
			}
			// finished writing
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
