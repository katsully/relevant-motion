{
    "real_time": true,
    "bones": [
        {
            "name": "RightUpperArm",
            "color1": "Green",
            "color2": "Green",
            "frequency": 100
        },
        {
            "name": "RightForeArm",
            "color1": "Blue",
            "color2": "Blue",
            "frequency": 100
        }
    ],
    "master_bone": "RightUpperArm",
    "special": {
        "bone": "RightUpperArm",
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
