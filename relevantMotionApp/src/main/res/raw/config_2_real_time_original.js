{
    "real_time": true,
    "bones": [
        {
            "name": "ChestBottom",
            "color1": "Blue",
            "color2": "Blue",
            "frequency": 20
        },
        {
            "name": "Hip",
            "color1": "Cyan",
            "color2": "Cyan",
            "frequency": 20
        }
    ],
    "master_bone": "ChestBottom",
    "special": {
        "bone": "ChestBottom",
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
