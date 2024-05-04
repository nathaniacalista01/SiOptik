from boxdetect import config
from boxdetect.pipelines import get_boxes
import matplotlib.pyplot as plt
import cv2
import os

cfg = config.PipelinesConfig()

# important to adjust these values to match the size of boxes on your image
# cfg.width_range = (110, 130)
# cfg.height_range = (110, 130)

cfg.width_range = (100, 180)
cfg.height_range = (100, 180)

# the more scaling factors the more accurate the results but also it takes more time to processing
# too small scaling factor may cause false positives
# too big scaling factor will take a lot of processing time
cfg.scaling_factors = [0.7]

# w/h ratio range for boxes/rectangles filtering
cfg.wh_ratio_range = (0.9, 1.1)

# group_size_range starting from 2 will skip all the groups
# with a single box detected inside (like checkboxes)
cfg.group_size_range = (2, 100)

# num of iterations when running dilation tranformation (to engance the image)
cfg.dilation_iterations = 0


# Specify the directory path
# directory = "input_img"
cwd = os.getcwd()
data_dir = os.path.join(cwd, "data")
out_dir = os.path.join(cwd, 'data-out')

count = 1
for file_name in os.listdir(data_dir):
    print(count)
    print(file_name)
    file_path = os.path.join(data_dir, file_name)
    
    if os.path.isfile(file_path) and file_path.endswith(".jpg"):
        rects, grouping_rects, image, output_image = get_boxes(
            file_path, cfg=cfg, plot=False
        )

        output_img_path = os.path.join(out_dir, file_name)
        cv2.imwrite(output_img_path, output_image)

        output_txt_path = (
            os.path.join(out_dir, file_name.removesuffix(".jpg")) + ".txt"
        )

        with open(output_txt_path, "a") as file:
            count_rect = 1
            for rect in rects:
                file.write("Rect "+ str(count_rect) + "\n")
                file.write(" ".join(map(str, rect)) + "\n")
                count_rect +=1
    count += 1


# plt.figure(figsize=(20, 20))
# plt.imshow(output_image)
# plt.show()
