{
    "real_time": true,
    "bones": [
        {
            "name": "LeftForeArm",
            "color1": "Green",
            "color2": "Green",
            "frequency": 20
        },
        {
            "name": "RightForeArm",
            "color1": "Blue",
            "color2": "Blue",
            "frequency": 20
        }
    ],
    "master_bone": "LeftForeArm",
    "special": {
        "bone": "LeftForeArm",
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
