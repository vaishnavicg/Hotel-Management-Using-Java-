package com.dailycodework.lakeSidehotel.controller;

import com.dailycodework.lakeSidehotel.exception.PhotoRetrievalException;
import com.dailycodework.lakeSidehotel.exception.ResourceNotFoundException;
import com.dailycodework.lakeSidehotel.model.BookedRoom;
import com.dailycodework.lakeSidehotel.model.Room;
import com.dailycodework.lakeSidehotel.response.BookingResponse;
import com.dailycodework.lakeSidehotel.response.RoomResponse;
import com.dailycodework.lakeSidehotel.service.BookingService;
import com.dailycodework.lakeSidehotel.service.IRoomService;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
//@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {
    private final IRoomService roomService;
    private final BookingService bookingService;

    //@CrossOrigin(origins = "http://localhost:5173")
    @PostMapping("/add/new-room")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RoomResponse> addNewRoom(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("roomType") String roomType,
            @RequestParam("pgName") String pgName,
            @RequestParam("pgId") long  pgId,
            @RequestParam("location") String location,
            @RequestParam("roomPrice") BigDecimal roomPrice) throws SQLException, IOException {

        System.out.println("Received photo: " + photo.getOriginalFilename());
        System.out.println("Received roomType: " + roomType);
        System.out.println("Received pgName: " + pgName);
        System.out.println("Received pgId: " + pgId);
        System.out.println("Received location: " + location);
        System.out.println("Received roomPrice: " + roomPrice);
        Room savedRoom = roomService.addNewRoom(photo, roomType, roomPrice,pgId,pgName,location);
        RoomResponse response = new RoomResponse(savedRoom.getId(), savedRoom.getRoomType(),savedRoom.getRoomPrice(),savedRoom.getPgId(),savedRoom.getPgName(),savedRoom.getLocation());
        return ResponseEntity.ok(response);
    }


    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/room/types")
    public List<String> getRoomTypes() {
        return roomService.getAllRoomTypes();
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/all-rooms")
    public ResponseEntity<List<RoomResponse>> getAllRooms() throws SQLException {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomResponse> roomResponses = new ArrayList<>();
        for (Room room : rooms) {
            byte[] photoBytes = roomService.getRoomPhotoByRoomId(room.getId());
            if (photoBytes != null && photoBytes.length > 0) {
                String base64Photo = Base64.encodeBase64String(photoBytes);
                RoomResponse roomResponse = getRoomResponse(room);
                roomResponse.setPhoto(base64Photo);
                roomResponses.add(roomResponse);
            }
        }
        return ResponseEntity.ok(roomResponses);
    }

   // @CrossOrigin(origins = "http://localhost:5173")
    @DeleteMapping("/delete/room/{roomId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId){
        roomService.deleteRoom(roomId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }




    //@CrossOrigin(origins = "http://localhost:5173")
    @PutMapping("/update/{roomId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long roomId,
                                                   @RequestParam(required = false)  String roomType,
                                                   @RequestParam(required = false) BigDecimal roomPrice,
                                                   @RequestParam(required = false) long pgId,
                                                   @RequestParam(required = false) String pgName,
                                                   @RequestParam(required = false) String  location,

                                                   @RequestParam(required = false) MultipartFile photo) throws SQLException, IOException {
        byte[] photoBytes = photo != null && !photo.isEmpty() ?
                photo.getBytes() : roomService.getRoomPhotoByRoomId(roomId);
        Blob photoBlob = photoBytes != null && photoBytes.length >0 ? new SerialBlob(photoBytes): null;
        Room theRoom = roomService.updateRoom(roomId, roomType, roomPrice,pgId,pgName,location, photoBytes);
        theRoom.setPhoto(photoBlob);
        RoomResponse roomResponse = getRoomResponse(theRoom);
        return ResponseEntity.ok(roomResponse);
    }


    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Optional<RoomResponse>> getRoomById(@PathVariable Long roomId){
        Optional<Room> theRoom = roomService.getRoomById(roomId);
        return theRoom.map(room -> {
            RoomResponse roomResponse = getRoomResponse(room);
            return  ResponseEntity.ok(Optional.of(roomResponse));
        }).orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }




    @GetMapping("/available-rooms")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(
            @RequestParam("checkInDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam("checkOutDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate checkOutDate,
            @RequestParam("roomType") String roomType) throws SQLException {
        List<Room> availableRooms = roomService.getAvailableRooms(checkInDate, checkOutDate, roomType);
        List<RoomResponse> roomResponses = new ArrayList<>();
        for (Room room : availableRooms){
            byte[] photoBytes = roomService.getRoomPhotoByRoomId(room.getId());
            if (photoBytes != null && photoBytes.length > 0){
                String photoBase64 = Base64.encodeBase64String(photoBytes);
                RoomResponse roomResponse = getRoomResponse(room);
                roomResponse.setPhoto(photoBase64);
                roomResponses.add(roomResponse);
            }
        }
        if(roomResponses.isEmpty()){
            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.ok(roomResponses);
        }
    }










    private RoomResponse getRoomResponse(Room room) {
        List<BookedRoom> bookings = getAllBookingsByRoomId(room.getId());

        /*List<BookingResponse> bookingInfo = bookings
                .stream()
                .map(booking -> new BookingResponse(
                        booking.getBookingId(),
                 booking.getBookingConfirmationCode())).toList();*/
        byte[] photoBytes = null;
        Blob photoBlob = room.getPhoto();
        if (photoBlob != null) {
            try {
                photoBytes = photoBlob.getBytes(1, (int) photoBlob.length());
            } catch (SQLException e) {
                throw new PhotoRetrievalException("Error retrieving photo");
            }
        }
        return new RoomResponse(room.getId(),
                room.getRoomType(),
                room.getRoomPrice(),
                room.isBooked(),
                photoBytes,
                room.getPgId(),
                room.getPgName(),
                room.getLocation()
                //bookingInfo
        );
    }

    private List<BookedRoom> getAllBookingsByRoomId(Long roomId) {
        return bookingService.getAllBookingsByRoomId(roomId);

    }



}
