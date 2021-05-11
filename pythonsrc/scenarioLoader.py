import json
import numpy as np
import os


class scenarioLoader:
    """
    This class loads the given scenario file and supports adding pedestrian at a given position
    """

    def __init__(self, scenario_file):
        """Initialize a scenarioLoader object

        Args:
            scenario_file (string): The path to a vadere scenario file.
        """
        self.scenario_file = scenario_file
        self.load()

    def load(self):
        """Load the vadere scenario file from json format into a dictionary
        """
        with open(self.scenario_file, "r") as myScenario:
            self.scenario_file_dict = json.load(myScenario)

        self.dynamicElements_list = self.scenario_file_dict[
            "scenario"]["topography"]["dynamicElements"]

    def addPed(self, x=0, y=0, id=-1, speed=np.random.normal(1.34, 0.26), targetIDs=[]):
        """Add a pedestrian into the dynamicElements list.

        Args:
            x (int, optional): x coordinate of the pedestrian. Defaults to 0.
            y (int, optional): y coordinate of the pedestrian. Defaults to 0.
            speed (float, optional): The freeFlowSpeed of the pedestrian. Defaults to np.random.normal(1.34, 0.26).
            targetIDs (list, optional): The list of target IDs. Defaults to [].
        """
        ped = {
            "attributes": {
                "id": id,
                "radius": 0.2,
                "densityDependentSpeed": False,
                "speedDistributionMean": 1.34,
                "speedDistributionStandardDeviation": 0.26,
                "minimumSpeed": 0.5,
                "maximumSpeed": 2.2,
                "acceleration": 2.0,
                "footstepHistorySize": 4,
                "searchRadius": 1.0,
                "walkingDirectionCalculation": "BY_TARGET_CENTER",
                "walkingDirectionSameIfAngleLessOrEqual": 45.0
            },
            "source": None,
            "targetIds": targetIDs,
            "nextTargetListIndex": 0,
            "isCurrentTargetAnAgent": False,
            "position": {
                "x": x,
                "y": y
            },
            "velocity": {
                "x": 0.0,
                "y": 0.0
            },
            "freeFlowSpeed": speed,
            "followers": [],
            "idAsTarget": -1,
            "isChild": False,
            "isLikelyInjured": False,
            "psychologyStatus": {
                "mostImportantStimulus": None,
                "threatMemory": {
                    "allThreats": [],
                    "latestThreatUnhandled": False
                },
                "selfCategory": "TARGET_ORIENTED",
                "groupMembership": "OUT_GROUP",
                "knowledgeBase": {
                    "knowledge": []
                }
            },
            "groupIds": [],
            "groupSizes": [],
            "trajectory": {
                "footSteps": []
            },
            "modelPedestrianMap": None,
            "type": "PEDESTRIAN"
        }

        self.dynamicElements_list.append(ped)

    def save(self, output_dir, scienario_name):
        """Save the scenario into a vadere scenario file as json format.

        Args:
            output_dir (string): The directory of the output file.
            scienario_name (string): Name of the output scenario.
        """

        self.scenario_file_dict["name"] = scienario_name

        output_file = os.path.join(output_dir, scienario_name+".scenario")

        with open(output_file, "w") as output_scenario:
            json.dump(self.scenario_file_dict, output_scenario, indent=4)
            print("The scenario is saved at", output_dir)
