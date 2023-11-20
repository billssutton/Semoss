from typing import Optional, Union, List, Dict, Any
from sentence_transformers import SentenceTransformer, util
from huggingface_hub import try_to_load_from_cache, _CACHED_NO_EXIST, snapshot_download, hf_hub_download
from transformers import AutoModel
from pathlib import Path
from ..tokenizers import HuggingfaceTokenizer

class LocalEmbedder():

    def __init__(
        self,
        model_name:str = None,
        **kwargs
    ) -> None:
        # TODO - remove or options once existing local embedders have been changed
        self.model_name = model_name or kwargs.get('model_path')
        
        assert self.model_name != None
        
        self.model_folder = self.get_physical_folder(repo_id = self.model_name)
        self.embedder = self.get_embedder(
            self.model_folder
        )

        self.tokenizer = HuggingfaceTokenizer(
            encoder_name = model_name, 
            max_tokens = kwargs.pop(
                'max_tokens', 
                None
            ),
            max_input_tokens = kwargs.pop(
                'max_input_tokens', 
                None
            )
        )
    
    def get_physical_folder(
        self, 
        repo_id:str
    ) -> str:
        filepath = try_to_load_from_cache(
            repo_id=repo_id, 
            filename='config.json'
        )

        if isinstance(filepath, str):
            # file exists and is cached
            return Path(filepath).parent.absolute()
        # elif filepath is _CACHED_NO_EXIST:
        #     # hopefully we are just missing the config file
        #     config_file = hf_hub_download(
        #         repo_id= repo_id,
        #         filename='config.json'
        #     )
        #     return Path(config_file).parent.absolute()
        else:
            try:
                # file does not exist so we need to download the repo
                return snapshot_download(repo_id)
            except:
                # really dont want to have to do this
                AutoModel.from_pretrained(repo_id)
                return try_to_load_from_cache(
                    repo_id=repo_id, 
                    filename='config.json'
                )
                
    def get_embedder(
        self,
        model_folder:str
    ):
        embedder = None
        try:
            embedder = SentenceTransformer(
                self.model_folder
            )
        except:
            # trust_remote_code is needed to use the encode method
            embedder = AutoModel.from_pretrained(
                self.model_folder, 
                trust_remote_code=True
            )

        return embedder

    def embeddings(
        self, 
        list_to_embed:List[str], 
        prefix=""
    ) -> List[float]:
        # Determine what object was bassed in so we can pre-configure it before making the call
        assert isinstance(list_to_embed, list) or isinstance(object_to_encode, str)
        

        embedded_tensor = self.embedder.encode(
            sentences = list_to_embed, 
        )
        
        total_tokens = sum([self.tokenizer.count_tokens(chunk) for chunk in list_to_embed])

        # TODO find a way to push back batches like OpenAI
        # THIS IS SLOW AS HECK
        # embedded_list = []
        # number_of_items_to_encode = len(list_to_embed)
        # for i in range(number_of_items_to_encode):
        #   embedded_list.append(
        #     self.embedder.encode(list_to_embed[i])
        #   )
        #   print(prefix + "Completed Embedding " + str(i) + "/" + str(number_of_items_to_encode) + " Chunks")
        
        return {
            'response':embedded_tensor.tolist(),
            'numberOfTokensInPrompt': total_tokens,
            'numberOfTokensInResponse': 0
        }

    def ask(
        self, 
        *args: Any, 
        **kwargs
    ) -> str:
        response = 'This model does not support text generation.'
        output_payload = {
            'response':response,
            'numberOfTokensInPrompt': 0,
            'numberOfTokensInResponse': len(self.tokenizer.encode(response))
        }
        return output_payload