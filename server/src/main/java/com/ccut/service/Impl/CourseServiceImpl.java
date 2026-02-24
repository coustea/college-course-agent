package com.ccut.service.Impl;

import com.ccut.entity.Course;
import com.ccut.mapper.CourseMapper;
import com.ccut.mapper.CourseVideoMapper;
import com.ccut.mapper.CourseDocumentMapper;
import com.ccut.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

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
        log.debug("执行方法：insert, 参数：course={}", course != null ? "courseName=" + course.getCourseName() : "null");
        try {
            int result = courseMapper.insert(course);
            log.info("课程插入成功：courseId={}, result={}", course != null ? course.getCourseId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("课程插入失败：courseName={}, error={}", course != null ? course.getCourseName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public Course insertWithImage(Course course, MultipartFile image) {
        log.debug("执行方法：insertWithImage, 参数：course={}, image={}", 
                course != null ? "courseName=" + course.getCourseName() : "null",
                image != null ? image.getOriginalFilename() : "null");
        long startTime = System.currentTimeMillis();
        try {
            if (image != null && !image.isEmpty()) {
                log.info("开始上传课程图片：filename={}, size={}", image.getOriginalFilename(), image.getSize());
                String url = handleFileUpload(image);
                course.setResourceUrl(url);
                log.info("课程图片上传成功：url={}", url);
            }

            int n = courseMapper.insert(course);
            long duration = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("课程添加成功（含图片）：courseId={}, 耗时={}ms", course != null ? course.getCourseId() : "null", duration);
                log.debug("方法返回：result={}", course);
                return course;
            }
            log.error("课程添加失败：courseId={}, 耗时={}ms", course != null ? course.getCourseId() : "null", duration);
            throw new RuntimeException("添加失败");
        } catch (Exception e) {
            log.error("课程添加异常（含图片）：courseName={}, error={}", course != null ? course.getCourseName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int deleteById(Long courseId) {
        log.debug("执行方法：deleteById, 参数：courseId={}", courseId);
        try {
            int result = courseMapper.deleteById(courseId);
            log.info("课程删除成功：courseId={}, result={}", courseId, result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("课程删除失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int updateById(Course course) {
        log.debug("执行方法：updateById, 参数：course={}", course != null ? "courseId=" + course.getCourseId() : "null");
        try {
            int result = courseMapper.updateById(course);
            log.info("课程更新成功：courseId={}, result={}", course != null ? course.getCourseId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("课程更新失败：courseId={}, error={}", course != null ? course.getCourseId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String updateWithImage(Course course, MultipartFile image) {
        log.debug("执行方法：updateWithImage, 参数：course={}, image={}", 
                course != null ? "courseId=" + course.getCourseId() : "null",
                image != null ? image.getOriginalFilename() : "null");
        long startTime = System.currentTimeMillis();
        try {
            if (course.getCourseId() == null) {
                log.error("参数验证失败：courseId 不能为空");
                throw new IllegalArgumentException("courseId 不能为空");
            }

            if (image != null && !image.isEmpty()) {
                log.info("开始更新课程图片：filename={}, size={}", image.getOriginalFilename(), image.getSize());
                String url = handleFileUpload(image);
                course.setResourceUrl(url);
                log.info("课程图片更新成功：url={}", url);
            }

            int n = courseMapper.updateById(course);
            long duration = System.currentTimeMillis() - startTime;
            if (n > 0) {
                log.info("课程更新成功（含图片）：courseId={}, 耗时={}ms", course.getCourseId(), duration);
                log.debug("方法返回：result=更新成功");
                return "更新成功";
            }
            log.warn("课程未找到或未变更：courseId={}, 耗时={}ms", course.getCourseId(), duration);
            throw new RuntimeException("未找到或未变更");
        } catch (Exception e) {
            log.error("课程更新异常（含图片）：courseId={}, error={}", course != null ? course.getCourseId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理文件上传
     */
    private String handleFileUpload(MultipartFile image) throws RuntimeException {
        log.debug("执行方法：handleFileUpload, 参数：filename={}", image.getOriginalFilename());
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

            String url = "/uploads/" + date + "/" + filename;
            log.info("文件上传成功：originalFilename={}, savedFilename={}, url={}", original, filename, url);
            log.debug("方法返回：url={}", url);
            return url;
        } catch (Exception e) {
            log.error("文件上传失败：filename={}, error={}", image.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public List<Course> selectAll() {
        log.debug("执行方法：selectAll");
        long startTime = System.currentTimeMillis();
        try {
            List<Course> courses = courseMapper.selectAll();
            log.info("查询所有课程成功：count={}", courses != null ? courses.size() : 0);
            if (courses != null) {
                for (Course c : courses) {
                    if (c != null && c.getCourseId() != null) {
                        c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                        c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                    }
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询所有课程完成（含视频和文档）：count={}, 耗时={}ms", courses != null ? courses.size() : 0, duration);
            log.debug("方法返回：result count={}", courses != null ? courses.size() : 0);
            return courses;
        } catch (Exception e) {
            log.error("查询所有课程失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Course selectById(Long courseId) {
        log.debug("执行方法：selectById, 参数：courseId={}", courseId);
        long startTime = System.currentTimeMillis();
        try {
            Course c = courseMapper.selectById(courseId);
            if (c != null && c.getCourseId() != null) {
                c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询课程详情成功：courseId={}, 耗时={}ms", courseId, duration);
            log.debug("方法返回：result={}", c != null ? "courseName=" + c.getCourseName() : "null");
            return c;
        } catch (Exception e) {
            log.error("查询课程详情失败：courseId={}, error={}", courseId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Course> searchByName(String name) {
        log.debug("执行方法：searchByName, 参数：name={}", name);
        try {
            List<Course> result = courseMapper.search(name, null);
            log.info("按名称搜索课程成功：name={}, count={}", name, result != null ? result.size() : 0);
            log.debug("方法返回：result count={}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("按名称搜索课程失败：name={}, error={}", name, e.getMessage(), e);
            throw e;
        }
    }

    public List<Course> search(String name, String description) {
        log.debug("执行方法：search, 参数：name={}, description={}", name, description);
        try {
            List<Course> courses = courseMapper.search(name, description);
            log.info("搜索课程成功：name={}, description={}, count={}", name, description, courses != null ? courses.size() : 0);
            if (courses != null) {
                for (Course c : courses) {
                    if (c != null && c.getCourseId() != null) {
                        c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                        c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                    }
                }
            }
            log.debug("方法返回：result count={}", courses != null ? courses.size() : 0);
            return courses;
        } catch (Exception e) {
            log.error("搜索课程失败：name={}, description={}, error={}", name, description, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean publishCourse(Long courseId, Long teacherId) {
        log.debug("执行方法：publishCourse, 参数：courseId={}, teacherId={}", courseId, teacherId);
        long startTime = System.currentTimeMillis();
        try {
            // 验证课程是否属于该教师
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                log.error("课程不存在：courseId={}", courseId);
                throw new RuntimeException("课程不存在");
            }
            if (!course.getTeacherId().equals(teacherId)) {
                log.error("无权发布此课程：courseId={}, teacherId={}, courseTeacherId={}", courseId, teacherId, course.getTeacherId());
                throw new IllegalArgumentException("无权发布此课程");
            }

            // 更新发布状态
            boolean result = courseMapper.updatePublishStatus(courseId, "published") > 0;
            long duration = System.currentTimeMillis() - startTime;
            log.info("课程发布成功：courseId={}, 耗时={}ms", courseId, duration);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("课程发布失败：courseId={}, teacherId={}, error={}", courseId, teacherId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean unpublishCourse(Long courseId, Long teacherId) {
        log.debug("执行方法：unpublishCourse, 参数：courseId={}, teacherId={}", courseId, teacherId);
        long startTime = System.currentTimeMillis();
        try {
            // 验证课程是否属于该教师
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                log.error("课程不存在：courseId={}", courseId);
                throw new RuntimeException("课程不存在");
            }
            if (!course.getTeacherId().equals(teacherId)) {
                log.error("无权取消发布此课程：courseId={}, teacherId={}, courseTeacherId={}", courseId, teacherId, course.getTeacherId());
                throw new IllegalArgumentException("无权取消发布此课程");
            }

            // 更新发布状态
            boolean result = courseMapper.updatePublishStatus(courseId, "draft") > 0;
            long duration = System.currentTimeMillis() - startTime;
            log.info("课程取消发布成功：courseId={}, 耗时={}ms", courseId, duration);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("课程取消发布失败：courseId={}, teacherId={}, error={}", courseId, teacherId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Course> getCoursesByTeacherId(Long teacherId) {
        log.debug("执行方法：getCoursesByTeacherId, 参数：teacherId={}", teacherId);
        long startTime = System.currentTimeMillis();
        try {
            List<Course> courses = courseMapper.selectByTeacherId(teacherId);
            log.info("查询教师课程成功：teacherId={}, count={}", teacherId, courses != null ? courses.size() : 0);
            if (courses != null) {
                for (Course c : courses) {
                    if (c != null && c.getCourseId() != null) {
                        c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                        c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                    }
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询教师课程完成（含视频和文档）：teacherId={}, 耗时={}ms", teacherId, duration);
            log.debug("方法返回：result count={}", courses != null ? courses.size() : 0);
            return courses;
        } catch (Exception e) {
            log.error("查询教师课程失败：teacherId={}, error={}", teacherId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Course> getPublishedCourses() {
        log.debug("执行方法：getPublishedCourses");
        long startTime = System.currentTimeMillis();
        try {
            List<Course> courses = courseMapper.selectByPublishStatus("published");
            log.info("查询已发布课程成功：count={}", courses != null ? courses.size() : 0);
            if (courses != null) {
                for (Course c : courses) {
                    if (c != null && c.getCourseId() != null) {
                        c.setVideos(courseVideoMapper.findByCourseId(c.getCourseId()));
                        c.setDocuments(courseDocumentMapper.findByCourseId(c.getCourseId()));
                    }
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("查询已发布课程完成（含视频和文档）：count={}, 耗时={}ms", courses != null ? courses.size() : 0, duration);
            log.debug("方法返回：result count={}", courses != null ? courses.size() : 0);
            return courses;
        } catch (Exception e) {
            log.error("查询已发布课程失败：error={}", e.getMessage(), e);
            throw e;
        }
    }
}
