from dataclasses import dataclass
from dataclasses_json import dataclass_json, LetterCase


@dataclass_json(letter_case=LetterCase.CAMEL)
@dataclass
class StartContainer:
    task_id: str
    data_location: object
    code_location: object
    code_hash: str


@dataclass_json
@dataclass
class AnalyzeArtifact:
    permission_id: str
