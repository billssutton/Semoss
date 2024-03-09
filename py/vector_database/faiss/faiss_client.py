from typing import List, Dict, Union, Optional, Any, Tuple
from datasets import Dataset, concatenate_datasets, load_dataset, disable_caching, Value
import pandas as pd
import faiss
import numpy as np
from ..encoders import *
import pickle
import os
import glob
from genai_client.tokenizers.huggingface_tokenizer import HuggingfaceTokenizer
import gaas_gpt_model as ggm
from ..constants import ENCODING_OPTIONS
from logging_config import get_logger
from threading import current_thread


class FAISSSearcher():
    '''
    The primary class for a faiss database classes and searching document embeddings
    '''

    datasetType = 'datasets'

    def __init__(
        self, 
        embeddings_engine,
        keywords_engine,
        tokenizer,
        metric_type_is_cosine_similarity:bool,
        base_path = None,
        reranker="BAAI/bge-reranker-base"
    ):
        # if df is None and ds is None:
        #  return "Both dataframe and dataset cannot be none"

        self.init_device()
        self.ds = None

        self.encoded_vectors = None
        self.vector_dimensions = None

        self.embeddings_engine = embeddings_engine
        self.keyword_engine = keywords_engine
        
        self.tokenizer = tokenizer

        self.base_path = base_path

        self.metric_type_is_cosine_similarity = metric_type_is_cosine_similarity
        self.default_sort_direction = False if self.metric_type_is_cosine_similarity else True
        
        # disable reranking by default
        # do this while checking it in
        self.rerank = True
        self.reranker_model = None
        self.reranker_gaas_model = None
        self.reranker_tok = None
        self.reranker = reranker
        
        # disable caching within the shell so that engines can be exported
        disable_caching()
        
        self.class_logger = get_logger(__name__)

    def __getattr__(self, name: str):
        return self.__dict__[f"_{name}"]
  
    def __setattr__(self, name:str, value:Any):
        '''
        Enfore types for specific attributes
        '''
        if name == 'encoded_vectors' or value != None:
            if name in ['ds']:
                if not isinstance(value, (pd.DataFrame, Dataset)):
                    raise TypeError(f"{name} must be a pd.DataFrame or Dataset")
            elif name in ['embeddings_engine', 'keyword_engine']:
                pass
                # if not isinstance(value, EncoderInterface):
                #       raise TypeError(f"{name} must be an instance of EncoderInterface")
            elif name in ['encoded_vectors']:
                if (np.any(value) != None) and not isinstance(value, np.ndarray) :
                    raise TypeError(f"{name} must be a np.ndarray")
            elif name in ['vector_dimensions']:
                if not isinstance(value, tuple):
                    raise TypeError(f"{name} must be a tuple")         
            elif name in ['base_path']:
                if not isinstance(value, str):
                    raise TypeError(f"{name} must be a string")
          
        self.__dict__[f"_{name}"] = value
   
    def _concatenate_columns(
        self, 
        row, 
        columns_to_index=None, 
        target_column=None, 
        separator="\n"
    ) -> Dict:
        text = ""
        for col in columns_to_index:
            text += str(row[col])
            text += separator
        return {target_column : text}
    
    def init_device(self):
        '''
        Utility method to determine whether or not the devie running the interpreter has a gpu
        '''
        import torch
        if torch.cuda.is_available():       
            self.device = torch.device("cuda")
            #print("Using GPU.")
        else:
            #print("No GPU available, using the CPU instead.")
            self.device = torch.device("cpu")
    
    def nearestNeighbor(
        self, 
        question: str,
        insight_id:str,
        filter: Optional[str] = None,
        results: Optional[int] = 5, 
        columns_to_return: Optional[List[str]] = None, 
        return_threshold: Optional[Union[int,float]] = 1000, 
        ascending : Optional[bool] = None,
        total_results: Optional[int] = 10 # this is used for reranking
    ) -> List[Dict]:
        '''
        Find the closest match(es) between the question bassed in and the embedded documents using Euclidena Distance.

        Args:
        question(`str`):
            The string you are trying to match against the embedded documents
        results(`Optional[int]`, *optional*):
            The number of matches under the threshold that will be returned
        columns_to_return(`List[str]`):
            A list of column names that will be sent back in the return payload.
            Example:
            # Given the following dataset
            >>> dataset
            Dataset({
                features: ['doc_index', 'content', 'tokens', 'url'],
                num_rows: 902
            })

            # if columns_to_return = None, then all four columns will be returned

            # if columns_to_return = ['doc_index']

            >>> FAISSearcher.nearestNeighbor(
            ...     question = 'Sample',
            ...     columns_to_return = ['doc_index'],
            ...     results = 1
            ... )
            [{'Score':0.23, "doc_index":"<theDocIndexThatMathced"}]
        return_threshold(`Optional[Union[int,float]]`):
            A numerical value that specifies what Score should be less than.
        ascending(`Optional[bool]`):
            A boolean flag to return results in ascending order or not. Default is True

        Return:
            `List[Dict]` consisting of Score and columns

        Example:
            >>> faissSearcherObj.nearestNeighbor(
            ...     question="""How is the president chosen""",
            ...     results = 3,
            ...     columns_to_return = ['doc_index'],
            ...     return_threshold = 1.0,
            ...     ascending = False
            ... )
            [{Score=0.9867115616798401, doc_index=1420-deloitte-independence_11_text}, 
            {Score=0.9855965375900269, doc_index=1420-deloitte-independence_10_text}]
        '''
        # if columns_to_return is None, then by default we return all columns
        if(columns_to_return is None):
            columns_to_return = list(self.ds.features)

        # make sure the encoder class is loaded and get the embeddings (vector) for the tokens
        # search_vector = self.embeddings_engine.get_embeddings([question])
        # query_vector = np.array([search_vector])

        search_vector = self.embeddings_engine.embeddings(
            strings_to_embed = [question], 
            insight_id = insight_id
        )
        
        query_vector = np.array(search_vector[0]['response'], dtype=np.float32)
        assert query_vector.shape[0] == 1

        # check to see if need to normalize the vector
        if isinstance(self.tokenizer, HuggingfaceTokenizer) or self.metric_type_is_cosine_similarity:
            faiss.normalize_L2(query_vector)

        # perform the faiss search. Scores returned are Euclidean distances
        # euclidean_distances - the measurement score between the embedded question and the Approximate Nearest Neighbor (ANN)
        # ann_index - the index location of the Approximate Nearest Neighbor (ANN)

        if not isinstance(results, int):
            results = int(results)
            
        if not self.rerank:
          total_results = results
        
        # If a filter was passed in then we need to get the indexes
        if filter != None:
            filter_ids = self._filter_dataset(filter)
            id_selector = faiss.IDSelectorArray(filter_ids)
            euclidean_distances, ann_index = self.index.search(
                query_vector, 
                k = total_results, 
                params=faiss.SearchParametersIVF(sel=id_selector)
            )
        else:
            euclidean_distances, ann_index = self.index.search( 
                query_vector, 
                k = total_results
            )

        euclidean_distances = euclidean_distances[0]
        ann_index = ann_index[0]

        if self.rerank:
          final_output = self.do_rerank(question=question, 
          euclidean_distances=euclidean_distances, 
          ann_index=ann_index, 
          result_count=results, 
          columns_to_return=columns_to_return, 
          ascending=ascending)
          return final_output

        else:
          # this is a safety check to make sure we are only returning good vectors if the limit was too high
          if self.vector_dimensions[0] < results:
              # Find the index of the first occurrence of -1
              index_of_minus_one = np.where(ann_index == -1)[0]
              # If -1 is not found, index_of_minus_one will be an empty array
              # In that case, we keep the original array, otherwise, we slice it
              if len(index_of_minus_one) > 0:
                  ann_index = ann_index[:index_of_minus_one[0]]
                  euclidean_distances = euclidean_distances[:index_of_minus_one[0]]

          # create the data
          samples_df = pd.DataFrame(
              {
                  'distances': euclidean_distances, 
                  'ann': ann_index
              }
          )
          samples_df.sort_values(
              "distances", 
              ascending = (ascending if ascending is not None else self.default_sort_direction), 
              inplace=True
          )
          samples_df = samples_df[samples_df['distances'] <= return_threshold]
      
          # create the response payload by adding the relevant columns from the dataset
          final_output = []
          
          # see if rerank is enabled
          # if so run through reranking this
          # and then limit to the final result 
          
          for _, row in samples_df.iterrows():
              output = {}
              output.update({'Score' : row['distances']})
              data_row = self.ds[int(row['ann'])]
              for col in columns_to_return:
                  output.update({col:data_row[col]})
              final_output.append(output)
        
          return final_output
    
    def _filter_dataset(self, filter:str) -> List[int]:
        if isinstance(self.ds, Dataset):
            filterDf = self.ds.to_pandas()
        else:
            filterDf = self.ds

        return filterDf.query(filter).index.to_list()

    def load_dataset(
        self, 
        dataset_location:str
    ) -> None:
        '''
        Utility method to load stored datasets into the object. 

        Args:
        dataset_location(`str`):
            The file path to the stored dataset. Currently only csv and pkl file types are supported

        Returns:
        `None`
        '''
        self.ds = self._load_dataset(dataset_location = dataset_location)

    def _load_dataset(
        self, 
        dataset_location:str
    ) -> Union[Dataset, pd.DataFrame]:
        '''
        Internal method to load the dataset based on its file type

        Args:
        dataset_location(`str`):
            The file path to the stored dataset. Currently only csv and pkl file types are supported

        Returns:
        `None`
        '''
        if (dataset_location.endswith('.csv')):
            if (FAISSSearcher.datasetType == 'pandas'):
                for encoding in ENCODING_OPTIONS:
                    try:
                        temp_df = pd.read_csv(dataset_location, encoding = encoding)
                        loaded_dataset = Dataset.from_pandas(
                            temp_df
                        )
                        break
                    except:
                        continue
                else:
                    # The else clause is executed if the loop completes without encountering a break
                    raise Exception("Unable to read the file with any of the specified encodings")
            else:   
                try:  
                    loaded_dataset = Dataset.from_csv(
                        path_or_paths = dataset_location, 
                        encoding ='iso-8859-1',
                        keep_in_memory = True
                    )
                except:
                    for encoding in ENCODING_OPTIONS:
                        try:
                            temp_df = pd.read_csv(dataset_location, encoding = encoding)
                            loaded_dataset = Dataset.from_pandas(
                                temp_df
                            )
                            break
                        except:
                            continue
                    else:
                        # The else clause is executed if the loop completes without encountering a break
                        raise Exception("Unable to read the file with any of the specified encodings")  

        elif (dataset_location.endswith('.pkl')):
            with open(dataset_location, "rb") as file:
                loaded_dataset = pickle.load(file)
        else:
            raise ValueError("Dataset creation for provided file type has not been defined")
    
        assert isinstance(loaded_dataset, (Dataset, pd.DataFrame))
        
        if (FAISSSearcher.datasetType == 'pandas'):
            dataset_columns = loaded_dataset.columns
        else:
            # Dataset
            dataset_columns = list(loaded_dataset.features)
        
        extracted_with_cfg = all(col in dataset_columns for col in ['Source','Divider', 'Part', 'Tokens','Content'])
        if isinstance(loaded_dataset, Dataset) and extracted_with_cfg:
            
            if 'Modality' not in dataset_columns:
                loaded_dataset = loaded_dataset.add_column("Modality", ['text' for i in range(loaded_dataset.num_rows)])
            
            # to be safe, force all columns
            new_features = loaded_dataset.features.copy()
            new_features["Source"] = Value(dtype='string', id=None)
            new_features["Divider"] = Value(dtype='string', id=None)
            new_features["Part"] = Value(dtype='string', id=None)
            new_features["Tokens"] = Value(dtype='int64', id=None)
            new_features["Content"] = Value(dtype='string', id=None)
            loaded_dataset = loaded_dataset.cast(new_features)
                
        elif isinstance(loaded_dataset, pd.DataFrame) and extracted_with_cfg:
            if 'Modality' not in dataset_columns:
                loaded_dataset["Modality"] = 'text'
            
            # to be safe, force all columns
            loaded_dataset["Source"] = loaded_dataset["Source"].astype(str)
            loaded_dataset["Divider"] = loaded_dataset["Divider"].astype(str)
            loaded_dataset["Part"] = loaded_dataset["Part"].astype(str)
            loaded_dataset["Tokens"] = loaded_dataset["Tokens"].astype(int)
            loaded_dataset["Content"] = loaded_dataset["Content"].astype(str)
        
        return loaded_dataset

    def save_dataset(
        self, 
        dataset_location: str
    ) -> None:
        '''
        Utility method to save datasets from object onto the disk. 

        Args:
        dataset_location(`str`):
            The file path to the write the dataset.

        Returns:
        `None`
        '''
        with open(dataset_location, "wb") as file:
            pickle.dump(self.ds, file)

    def load_encoded_vectors(
        self, 
        encoded_vectors_location: str
    ) -> None:
        '''
        Utility method to load stored embeddings from the disk.

        Args:
        encoded_vectors_location(`str`):
            The file path to the stored embeddings file. Currently only npy and pkl file types are supported

        Returns:
        `None`
        '''
        self.encoded_vectors = self._load_encoded_vectors(encoded_vectors_location = encoded_vectors_location)
        self.vector_dimensions = self.encoded_vectors.shape
    
        if self.metric_type_is_cosine_similarity:
            self.index = faiss.index_factory(self.vector_dimensions[1], "Flat", faiss.METRIC_INNER_PRODUCT)
        else:
            self.index = faiss.IndexFlatL2(self.vector_dimensions[1])
        # if isinstance(self.embeddings_engine, HuggingfaceTokenizer):
        #   faiss.normalize_L2(self.encoded_vectors)
        self.index.add(self.encoded_vectors)

    def _load_encoded_vectors(
        self, 
        encoded_vectors_location: str
    ) -> np.ndarray:
        '''
        Internal method to load stored embeddings from the disk

        Args:
        encoded_vectors_location(`str`):
            The file path to the stored embeddings file. Currently only npy and pkl file types are supported

        Returns:
        `None`
        '''
        if (encoded_vectors_location.endswith('.npy')):
            encoded_vectors = np.load(encoded_vectors_location)
        else:
            with open(encoded_vectors_location, "rb") as file:
                encoded_vectors = pickle.load(file)

        assert isinstance(encoded_vectors, np.ndarray)
        return encoded_vectors

    def save_encoded_vectors(
        self, 
        encoded_vectors_location: str
    ) -> None :
        '''
        Utility method to save embeddings from object onto the disk. 

        Args:
        encoded_vectors_location(`str`):
            The file path to the write the dataset.

        Returns:
        `None`
        '''
        with open(encoded_vectors_location, "wb") as file:
            pickle.dump(self.encoded_vectors, file)

    def _concatenate_datasets(
        self,
        datasets: Union[List[Dataset], List[pd.DataFrame]],
    ) -> Union[Dataset, pd.DataFrame]:
        '''
        Interal utility method to concatenate datasets depending on the class type. Either pandas.DataFrame or datasets.Dataset

        Args:
        datasets(`Union[List[Dataset], List[pd.DataFrame]]`):
            A list of datasets where all the datasets of only of one type. Either pandas.DataFrame or datasets.Dataset 

        Returns:
        `Union[Dataset, pd.DataFrame]`
        '''
        if (FAISSSearcher.datasetType == 'pandas'):
            return pd.concat(datasets, axis=1, verify_integrity=True)
        else:
            return concatenate_datasets(datasets)

    def addDocumet(
        self, 
        documentFileLocation: List[str], 
        insight_id:str,
        columns_to_index: Optional[List[str]], 
        columns_to_remove: Optional[List[str]] = [],
        target_column: Optional[str] = "text", 
        separator: Optional[str] = ',',
        keyword_search_params: Optional[Dict] = {},
    ) -> Dict:
        '''
        Given a path to a CSV document, perform the following tasks:
        - concatenate the columns the embeddings should be created from
        - get the embeddings for all the extracted chunks in the document
        - `Optional` - remove the columns that are not supposed to be stored based on columns_to_remove param
        - write out both the dataset and embeddings objects onto the disk so they can be reloaded or removed

        Args:
        documentFileLocation(`List[str]`):
            A list of document file location to create embeddings from
        columns_to_index(`List[str]`):
            A list of column names to create the index from. These columns will be concatenated.
        columns_to_remove(`List[str]`):
            A list of column names that should not be stored in the dataset. This will never be returned in nearestNeighbor search because they will no longer exist.
        target_column(`str`):
            The column name for the concatenated columns from which the embeddings will be created
        separator(`str`):
            The character to use as a delimeter between columns for the concatenated column that the embeddings will be created from
        keyword_search_params (`Dict`):
            A dictionary containing the keyword search parameters

        Returns:
        `None`
        '''        
        # make sure they are all in indexed_files dir
        assert {os.path.basename(os.path.dirname(path)) for path in documentFileLocation} == {'indexed_files'}

        # create a list of the documents created so that we can push the files back to the cloud
        createDocumentsResponse = {
            'createdDocuments':[],
            'documentsWithLargerChunks': {}
        }
    
        # loop through and embed new docs
        for document in documentFileLocation:
            # Get the directory path and the base filename without extension
            directory, base_filename = os.path.split(document)
            file_name_without_extension, file_extension = os.path.splitext(base_filename)
            new_file_extension = ".pkl"

            # Create the Dataset for every file
            dataset = self._load_dataset(dataset_location=document)

            if (columns_to_index == None or len(columns_to_index) == 0):
                columns_to_index = list(dataset.features)

            # save the dataset, this is for efficiency after removing docs
            new_file_path = os.path.join(
                directory, 
                file_name_without_extension + '_dataset' + new_file_extension
            )

            # if applicable, create the concatenated columns
            if (dataset.num_rows > 0):
                dataset = dataset.map(
                    self._concatenate_columns,           
                    fn_kwargs = {
                    "columns_to_index": columns_to_index, 
                    "target_column": target_column, 
                    "separator":separator
                    }
                )
                
                # transform chunks into keywords
                if keyword_search_params != None and keyword_search_params.pop('keywordSearch', None) is True:
                    keywords_for_target_col = self.keyword_engine.model(
                        input = dataset[target_column],
                        insight_id = insight_id,
                        param_dict = keyword_search_params
                    )[0]
                    #dataset = dataset.add_column(target_column, keywords_for_target_col)
                    dataset = dataset.remove_columns(column_names= target_column)
                    dataset = dataset.add_column(target_column, keywords_for_target_col)

                # need to check that the chunks are not greater than what the tokenizer can handle
                chunks_with_larger_tokens = self._check_chunks_token_size(dataset[target_column])
                createDocumentsResponse['documentsWithLargerChunks'][document] = chunks_with_larger_tokens

                # get the embeddings for the document
                #vectors = self.embeddings_engine.get_embeddings(dataset[target_column])
                vectors = self.embeddings_engine.embeddings(
                    strings_to_embed = dataset[target_column], 
                    insight_id = insight_id
                )
                vectors = np.array(vectors[0]['response'], dtype=np.float32)
                assert vectors.ndim == 2

                columns_to_remove.append(target_column)
                columns_to_drop = list(set(columns_to_remove).intersection(set(dataset.features)))
                dataset = dataset.remove_columns(column_names= columns_to_drop)

                with open(new_file_path, "wb") as file:
                    pickle.dump(dataset, file)
                
                # add the created dataset file path
                createDocumentsResponse['createdDocuments'].append(new_file_path)
                
                # normalize the vectors if using huggingface
                if isinstance(self.tokenizer, HuggingfaceTokenizer) or self.metric_type_is_cosine_similarity:
                    faiss.normalize_L2(vectors)

                # write out the vectors with the same file name
                # Change the file extension to ".pkl"
                new_file_path = os.path.join(
                    directory, 
                    file_name_without_extension + '_vectors' + new_file_extension
                )
                with open(new_file_path, "wb") as file:
                    pickle.dump(vectors, file)

                # add the created embeddings file path
                createDocumentsResponse['createdDocuments'].append(new_file_path)

                # TODO need to update the flow for how we instatiate
                if (np.any(self.encoded_vectors) == None):
                    self.encoded_vectors = np.copy(vectors)
                    self.vector_dimensions = self.encoded_vectors.shape
                else:
                    # make sure the dimensions are the same
                    assert self.vector_dimensions[1] == vectors.shape[1]
                    self.encoded_vectors = np.concatenate([self.encoded_vectors, vectors], axis=0)

        master_indexClass_files, corrupted_file_sets = self.createMasterFiles(
            path_to_files=os.path.dirname(os.path.dirname(documentFileLocation[0]))
        )
        
        for corrupted_set in corrupted_file_sets:
            for file_path in corrupted_set:
                createDocumentsResponse['createdDocuments'].remove(file_path)
        
        createDocumentsResponse['createdDocuments'].extend(master_indexClass_files)

        return createDocumentsResponse

    def createMasterFiles(
        self, 
        path_to_files:str
    ) -> Tuple[str]:
        '''
        Create a master dataset and embeddings file based on the current documents. The main purpose of this is to improve startup runtime. 

        Args:
        path_to_files(`str`):
            The folder location of the indexed documents/datasets/embeddings

        Returns:
        `List[str]`
        '''
        created_documents, corrupted_docs, corrupted_file_sets = self._validateEmbeddingFiles(
            path_to_files=path_to_files,
        )
        
        return created_documents, corrupted_file_sets
    
    def _validateEmbeddingFiles(
        self,
        path_to_files: str,
        delete: bool = True
    ) -> Dict:
        # Path to the directory containing the files
        #'C:/Users/ttrankle/Documents/Semoss/Client Work/MDE/cases/6B04BF9F-904E-4B2A-A97B-16D54E3F89DF__5fc5b497-4ea9-4369-b293-59d774b15697/'

        documents_files_path = os.path.join(path_to_files, 'documents')
        indexed_files_path = os.path.join(path_to_files, 'indexed_files')

        # List all pdfs files in the directory
        source_documents = glob.glob(os.path.join(documents_files_path, "*"))

        valid_datasets_and_vectors = []
        corrupted_file_sets = []
        corrupted_docs = {}
        created_documents = []
        
        for full_source_path in source_documents:
            # get the basename of the file
            # all csvs, datasets and vectors should contain this base name
            pdf_file_name = os.path.basename(full_source_path)
            base_filename = os.path.splitext(pdf_file_name)[0]
            
            # get the file names for the dataset and vectors
            csv_file_name = base_filename + ".csv"
            dataset_file_name = base_filename + "_dataset.pkl"
            vector_file_name = base_filename + "_vectors.pkl"
            
            full_csv_path = os.path.join(indexed_files_path, csv_file_name)
            full_dataset_path = os.path.join(indexed_files_path, dataset_file_name)
            full_vector_path = os.path.join(indexed_files_path, vector_file_name)

            # if all the file paths exist, then create the tuple
            if os.path.exists(full_dataset_path) and os.path.exists(full_vector_path):
                # the next step is to validate non of these files are corrupted by attempting to load them all in
                
                try:
                    # try load the dataset
                    dataset = self._load_dataset(dataset_location=full_dataset_path)
                except Exception as e:
                    try:
                        # we can try save the dataset again from the csv
                        dataset = self._load_dataset(dataset_location=full_csv_path)
                        with open(full_dataset_path, "wb") as file:
                            pickle.dump(dataset, file)
                    except:
                        
                        corrupted_file_sets.append(
                            (full_csv_path, full_dataset_path, full_vector_path, full_source_path)
                        )
                        corrupted_docs[full_source_path] = "Couldn't load the csv file or save it as a dataset"
                        continue
                    
                    try:
                        # make sure we can load it in again
                        dataset = self._load_dataset(dataset_location=full_dataset_path)
                    except:
                        # we failed so record failure and continue on
                        corrupted_file_sets.append(
                            (full_csv_path, full_dataset_path, full_vector_path, full_source_path)
                        )
                        corrupted_docs[full_source_path] = "Couldn't load the dataset from the pickle file"
                        continue
                    
                try:
                    # try load the vectors
                    vectors = self._load_encoded_vectors(encoded_vectors_location=full_vector_path)
                except:
                    corrupted_file_sets.append(
                        (full_csv_path, full_dataset_path, full_vector_path, full_source_path)
                    )
                    corrupted_docs[full_source_path] = "Couldn't load the embeddings from the pickle file"
                    continue
                    
                # if we made it this far then all the files are not corrupted
                valid_datasets_and_vectors.append(
                    (dataset, vectors)
                )

        # bind the valid datasets and vectors
        if len(valid_datasets_and_vectors) > 0:
            self.ds = valid_datasets_and_vectors[0][0]
            self.encoded_vectors = valid_datasets_and_vectors[0][1]
            self.vector_dimensions = self.encoded_vectors.shape
            
            # loop through and concatenate the others if any
            for dataset, vectors in valid_datasets_and_vectors[1:]:
                self.ds = self._concatenate_datasets([self.ds, dataset])
                self.encoded_vectors = np.concatenate((self.encoded_vectors,vectors),axis=0)
                
            encoded_vectors_location = path_to_files + "/vectors.pkl"
            dataset_location = path_to_files + "/dataset.pkl"
            self.save_encoded_vectors(encoded_vectors_location = encoded_vectors_location)
            self.save_dataset(dataset_location = dataset_location)
            created_documents.append(encoded_vectors_location)
            created_documents.append(dataset_location)
            
            if (self.metric_type_is_cosine_similarity) and (self.vector_dimensions != None):
                self.index = faiss.index_factory(self.vector_dimensions[1], "Flat", faiss.METRIC_INNER_PRODUCT)
            elif (self.vector_dimensions != None):
                self.index = faiss.IndexFlatL2(self.vector_dimensions[1])

            if (self.encoded_vectors is not None):
                self.index.add(self.encoded_vectors)
            
        if delete:  
            for corrupted in corrupted_file_sets:
                for filename in corrupted:
                    try:
                        os.remove(filename)
                    except FileNotFoundError:
                        pass
                
        return created_documents, corrupted_docs, corrupted_file_sets
    
    def removeCorruptedFiles(
        self,
        path_to_files: str
    ) -> Dict:
        corrupted_files = self._validateEmbeddingFiles(
            path_to_files=path_to_files,
        )[1]
        
        return corrupted_files

    def _check_chunks_token_size(
        self, 
        strings_to_embed:List[str]
    ):
        max_token_length = self.tokenizer.get_max_token_length()
        number_of_chunks = len(strings_to_embed)
        chunks_with_higher_tokens = []
        for i in range(number_of_chunks):
            chunk =  strings_to_embed[i]
            tokens_in_chunk = self.tokenizer.count_tokens(chunk)
            if (tokens_in_chunk > max_token_length):
                chunks_with_higher_tokens.append(i)
    
        return chunks_with_higher_tokens
    
    def datasetsLoaded(
        self
    ) -> bool:
        '''
        Check if data was loaded in from the csv
        '''
        if (self.ds == None) or (list(self.ds.features) == []) or (len(list(self.ds.features)) == 0) or (self.ds.num_rows == 0):
            return False
        else:
            return True


    def do_rerank(self,
        question: str,
        euclidean_distances: list,
        ann_index: list,
        result_count: int,
        columns_to_return: Optional[List[str]] = None,
        ascending : Optional[bool] = None
      ):
      # reranks based on an algorithm and then finds 
      
      
      if self.reranker_gaas_model is None:
        self.init_reranker()

      
      samples_df = pd.DataFrame(
        {
            'distances': euclidean_distances, 
            'ann': ann_index
        }
      )
      
      #samples_df.sort_values(
      #    "distances", 
      #    ascending = (ascending if ascending is not None else self.default_sort_direction), 
      #    inplace=True
      #)
      #samples_df = samples_df[samples_df['distances'] <= return_threshold]
      # self.class_logger.warning(f"Return length is set to {len(euclidean_distances)}", extra={"stack": "BACKEND"})
  
      # create the response payload by adding the relevant columns from the dataset
      result_chunks = []
      
      # see if rerank is enabled
      # if so run through reranking this
      # and then limit to the final result 
      final_output = []
      
      reranker_call_success = True
      for _, row in samples_df.iterrows():
        output = {}
        output.update({'Score' : row['distances']})
        data_row = self.ds[int(row['ann'])]
        #self.class_logger.warning(f"Row to pick {int(row['ann'])}", extra={"stack": "BACKEND"})
        #self.class_logger.warning(f"[{str(data_row['Content'])}]", extra={"stack": "BACKEND"})
        for col in columns_to_return:
            #self.class_logger.warning(f"{col} {data_row[col]}", extra={"stack": "BACKEND"})
            output.update({col:data_row[col]})
        # this is not pythonic but let us try this for now
        #self.class_logger.warning(question, extra={"stack": "BACKEND"})
        try:
            if 'Content' in data_row.keys():
                content = data_row['Content']
            else:
                content = " ".join([str(val) for val in data_row.values()])
                
            score = self.cross_encode(
                [[question, content]]
            )
            output.update({'Sim': score})
        except:
            reranker_call_success = False
        
        final_output.append(output)

      # sort this by sim score
      if reranker_call_success:
        new_output = sorted(final_output, key=lambda x : x['Sim'], reverse=True)
      else:
        new_output = final_output
      
      # filter to the top x
      new_output = new_output[:result_count]

      return new_output
 
      # now comes the reranker 
    
    def cross_encode(self,
        pair: List[str]
    ):
        return self.reranker_gaas_model.model(input=pair)
    
    def init_reranker(self):
      # local model
      #self.reranker_gaas_model = ggm.ModelEngine(model_engine=reranker, local=True)
      self.reranker_gaas_model = ggm.ModelEngine(engine_id="30991037-1e73-49f5-99d3-f28210e6b95c12")
      
