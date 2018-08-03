{
    "real_time": true,
    "bones": [
        {
            "name": "RightLowerLeg",
            "color1": "Blue",
            "color2": "Blue",
            "frequency": 20
        },
        {
            "name": "RightForeArm",
            "color1": "Green",
            "color2": "Green",
            "frequency": 20
        },
        {
            "name": "LeftLowerLeg",
            "color1": "Yellow",
            "color2": "Yellow",
            "frequency": 20
        },
        {
            "name": "LeftForeArm",
            "color1": "Red",
            "color2": "Red",
            "frequency": 20
        }
    ],
    "master_bone": "RightForeArm",
    "special": {
        "bone": "RightForeArm",
        "orientation": "Front"
    },
    "constraints": [
        {
            "type": "INTERP",
            "target": "Tummy",
            "source": "ChestBottom",
            "f": 0.5
        },
        {
            "type": "INTERP",
            "target": "ChestTop",
            "source": "Hip",
            "f": -0.42
        }
    ]
}
