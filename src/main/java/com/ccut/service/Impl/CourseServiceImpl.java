package com.ccut.service.Impl;

import com.ccut.entity.Course;
import com.ccut.mapper.CourseMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 课程服务实现类
 */
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseVideoMapper courseVideoMapper;

    @Autowired
    private CourseDocumentMapper courseDocumentMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public int insert(Course course) {
        return courseMapper.insert(course);
    }

    @Override
    @Transactional
    public Course insertWithImage(Course course, MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            String url = handleFileUpload(image);
            course.setResourceUrl(url);
        }

        int n = courseMapper.insert(course);
        if (n > 0) {
            return course;
        }
        throw new RuntimeException("添加失败");
    }

    @Override
    public int deleteById(Long courseId) {
        return courseMapper.deleteById(courseId);
    }

    @Override
    public int updateById(Course course) {
        return courseMapper.updateById(course);
    }

    @Override
    @Transactional
    public String updateWithImage(Course course, MultipartFile image) {
        if (course.getCourseId() == null) {
            throw new IllegalArgumentException("courseId 不能为空");
        }

        if (image != null && !image.isEmpty()) {
            String url = handleFileUpload(image);
            course.setResourceUrl(url);
        }

        int n = courseMapper.updateById(course);
        if (n > 0) {
            return "更新成功";
        }
        throw new RuntimeException("未找到或未变更");
    }

    /**
     * 处理文件上传
     */
    private String handleFileUpload(MultipartFile image) throws RuntimeException {
        try {
            LocalDate date = LocalDate.now();

            Path baseDir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(baseDir);

            Path uploadDateDir = baseDir.resolve(date.toString());
            Files.createDirectories(uploadDateDir);

            String original = image.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.') + 1)
                    : "";

            String filename = UUID.randomUUID().toString().replace("-", "");
            if (!ext.isEmpty()) {
                filename += "." + ext;
            }

            Path target = uploadDateDir.resolve(filename);
            image.transferTo(target.toFile());

            return "/uploads/" + date + "/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public List<Course> selectAll() {
        List<Course> courses = courseMapper.selectAll();
        if (courses != null) {
            for (Course c : courses) {
                if (c != null && c.getCourseId() != null) {
                    c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                    c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                }
            }
        }
        return courses;
    }

    @Override
    public Course selectById(Long courseId) {
        Course c = courseMapper.selectById(courseId);
        if (c != null && c.getCourseId() != null) {
            c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
            c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
        }
        return c;
    }

    @Override
    public List<Course> searchByName(String name) {
        return courseMapper.search(name, null);
    }

    public List<Course> search(String name, String description) {
        List<Course> courses = courseMapper.search(name, description);
        if (courses != null) {
            for (Course c : courses) {
                if (c != null && c.getCourseId() != null) {
                    c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                    c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                }
            }
        }
        return courses;
    }
}
