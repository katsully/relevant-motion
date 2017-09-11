{
    "real_time": true,
    "bones": [
        {
            "name": "RightUpperArm",
            "color1": "Yellow",
            "color2": "Yellow",
            "frequency": 20
        },
        {
            "name": "RightForeArm",
            "color1": "Cyan",
            "color2": "Cyan",
            "frequency": 20
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
        }
    ]
}
